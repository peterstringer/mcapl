// ----------------------------------------------------------------------------
// Copyright (C) 2026 Peter Stringer, Louise A. Dennis
//
// This file is part of the Agent Infrastructure Layer (AIL)
//
// The AIL is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// The AIL is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with the AIL; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------
package ail.semantics.adaptive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import ail.syntax.ActionLogEntry;
import ail.syntax.Capability;
import ail.syntax.Literal;
import ail.syntax.Predicate;
import ail.syntax.Unifier;

/**
 * A breadth-first forward-search STRIPS planner over the agent's action descriptions, used to
 * find patches for plans that contain a deprecated action (Cardoso et al. 2019).
 *
 * Action descriptions may contain variables provided every variable in the action and its effects
 * is bound by its positive pre-conditions.  Breadth-first search returns a shortest patch.  The
 * search is bounded both in depth and in the number of states expanded so that the agent's
 * reasoning cycle is never blocked indefinitely.
 *
 * @author Peter Stringer
 */
public class StripsPlanner {
	/** Default bound on the length of a plan. */
	public static final int DEFAULT_MAX_DEPTH = 10;
	/** Default bound on the number of states expanded. */
	public static final int DEFAULT_MAX_NODES = 100000;

	private final int maxDepth;
	private final int maxNodes;

	/**
	 * An action description read as a STRIPS operator.
	 */
	public static final class Operator {
		final Predicate action;
		final StripsConditions pre;
		final StripsConditions effect;
		final boolean override;

		/**
		 * @param a the action.
		 * @param p the pre-conditions.
		 * @param e the effects: positive literals are added, negative literals deleted.
		 * @param o whether this is a description of a specific ground action that overrides
		 *  any more general description of that action.
		 */
		public Operator(Predicate a, StripsConditions p, StripsConditions e, boolean o) {
			action = a;
			pre = p;
			effect = e;
			override = o;
		}

		/**
		 * Read a capability as an operator.
		 * @param c
		 * @param override see {@link #Operator}.
		 * @return the operator, or null if the capability's conditions are outside the STRIPS fragment.
		 */
		public static Operator fromCapability(Capability c, boolean override) {
			StripsConditions p = StripsConditions.fromFormula(c.getPre());
			StripsConditions e = StripsConditions.fromFormula(c.getPost());
			if (p == null || e == null) {
				return null;
			}
			return new Operator(c.getCap(), p, e, override);
		}

		@Override
		public String toString() {
			return pre + " " + action + " " + effect;
		}
	}

	public StripsPlanner() {
		this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES);
	}

	public StripsPlanner(int maxdepth, int maxnodes) {
		maxDepth = maxdepth;
		maxNodes = maxnodes;
	}

	/**
	 * Find a shortest sequence of actions that transforms the initial state into one satisfying the goal.
	 * @param initial the ground literals true in the initial state.
	 * @param goal the goal.
	 * @param ops the available operators.
	 * @return the plan, an empty list if the goal is already satisfied, or null if no plan
	 *  was found within the search bounds.
	 */
	public List<Predicate> plan(Collection<Literal> initial, StripsConditions goal, List<Operator> ops) {
		Set<String> overridden = new HashSet<String>();
		for (Operator op: ops) {
			if (op.override && op.action.isGround()) {
				overridden.add(op.action.toString());
			}
		}

		TreeMap<String, Literal> start = new TreeMap<String, Literal>();
		for (Literal l: initial) {
			Literal plain = ActionLogEntry.stripAnnotations(l);
			start.put(plain.toString(), plain);
		}

		LinkedList<Node> frontier = new LinkedList<Node>();
		HashSet<String> visited = new HashSet<String>();
		frontier.add(new Node(start, Collections.<Predicate>emptyList()));
		visited.add(start.keySet().toString());
		int expanded = 0;

		while (! frontier.isEmpty()) {
			Node n = frontier.removeFirst();
			if (satisfies(n.state, goal)) {
				return n.plan;
			}
			if (n.plan.size() >= maxDepth || ++expanded > maxNodes) {
				continue;
			}
			for (Operator op: ops) {
				for (Unifier u: instantiations(op.pre.getPositive(), 0, new Unifier(), n.state)) {
					Node next = successor(n, op, u, overridden);
					if (next != null && visited.add(next.state.keySet().toString())) {
						frontier.addLast(next);
					}
				}
			}
		}
		return null;
	}

	private static final class Node {
		final TreeMap<String, Literal> state;
		final List<Predicate> plan;

		Node(TreeMap<String, Literal> s, List<Predicate> p) {
			state = s;
			plan = p;
		}
	}

	/**
	 * Apply an operator instantiated by a unifier, or return null if it is not applicable.
	 */
	private static Node successor(Node n, Operator op, Unifier u, Set<String> overridden) {
		Predicate act = (Predicate) op.action.clone();
		act.apply(u);
		if (! act.isGround()) {
			return null;
		}
		if (! op.override && overridden.contains(act.toString())) {
			return null;
		}
		for (Literal l: op.pre.getNegative()) {
			Literal g = ground(l, u);
			if (g == null || n.state.containsKey(g.toString())) {
				return null;
			}
		}
		TreeMap<String, Literal> s = new TreeMap<String, Literal>(n.state);
		for (Literal l: op.effect.getNegative()) {
			Literal g = ground(l, u);
			if (g == null) {
				return null;
			}
			s.remove(g.toString());
		}
		for (Literal l: op.effect.getPositive()) {
			Literal g = ground(l, u);
			if (g == null) {
				return null;
			}
			s.put(g.toString(), g);
		}
		ArrayList<Predicate> plan = new ArrayList<Predicate>(n.plan);
		plan.add(act);
		return new Node(s, plan);
	}

	private static Literal ground(Literal l, Unifier u) {
		Literal g = l.clone();
		g.apply(u);
		if (g.isGround()) {
			return g;
		}
		return null;
	}

	/**
	 * All ways of unifying a list of literals with literals in the state.
	 */
	private static List<Unifier> instantiations(List<Literal> ls, int i, Unifier u, TreeMap<String, Literal> state) {
		ArrayList<Unifier> out = new ArrayList<Unifier>();
		if (i == ls.size()) {
			out.add(u);
			return out;
		}
		for (Literal s: state.values()) {
			Unifier u2 = u.clone();
			Literal l = ls.get(i).clone();
			if (l.unifies(s, u2)) {
				out.addAll(instantiations(ls, i + 1, u2, state));
			}
		}
		return out;
	}

	private static boolean satisfies(TreeMap<String, Literal> state, StripsConditions goal) {
		for (Unifier u: instantiations(goal.getPositive(), 0, new Unifier(), state)) {
			boolean ok = true;
			for (Literal l: goal.getNegative()) {
				Literal g = l.clone();
				g.apply(u);
				if (state.containsKey(g.toString())) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return true;
			}
		}
		return false;
	}
}
