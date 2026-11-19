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
package ail.syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * An entry in an agent's action log: an action, the change in the agent's beliefs
 * between the start and end of its execution, and how the execution terminated.
 *
 *     < a, delta(B, B'), outcome >
 *
 * @author Peter Stringer
 */
public final class ActionLogEntry {
	/** How an action execution terminated. */
	public static enum Outcome { SUCCESS, FAILURE, ABORT }

	private final Predicate action;
	private final List<Literal> added;
	private final List<Literal> deleted;
	private final Outcome outcome;

	/**
	 * Whether this entry has already been used to learn a new description of its action.
	 * The failure count of an action is reset once a description has been learned.
	 */
	boolean consumed = false;

	/**
	 * Constructor.
	 * @param a the (ground) action that was executed.
	 * @param before the agent's beliefs when the action started.
	 * @param after the agent's beliefs when the action terminated.
	 * @param o the outcome.
	 */
	public ActionLogEntry(Predicate a, Collection<Literal> before, Collection<Literal> after, Outcome o) {
		TreeMap<String, Literal> b = index(before);
		TreeMap<String, Literal> aft = index(after);
		ArrayList<Literal> add = new ArrayList<Literal>();
		ArrayList<Literal> del = new ArrayList<Literal>();
		for (String k: aft.keySet()) {
			if (! b.containsKey(k)) {
				add.add(aft.get(k));
			}
		}
		for (String k: b.keySet()) {
			if (! aft.containsKey(k)) {
				del.add(b.get(k));
			}
		}
		action = a;
		added = Collections.unmodifiableList(add);
		deleted = Collections.unmodifiableList(del);
		outcome = o;
	}

	/**
	 * Index beliefs by their printed form, ignoring annotations (such as sources).
	 */
	private static TreeMap<String, Literal> index(Collection<Literal> ls) {
		TreeMap<String, Literal> m = new TreeMap<String, Literal>();
		for (Literal l: ls) {
			Literal plain = stripAnnotations(l);
			m.put(plain.toString(), plain);
		}
		return m;
	}

	/**
	 * A copy of a literal without annotations.
	 * @param l
	 * @return
	 */
	public static Literal stripAnnotations(Literal l) {
		return new Literal(! l.negated(), new Predicate(l));
	}

	public Predicate getAction() {
		return action;
	}

	/** Beliefs gained during the execution. */
	public List<Literal> getAdded() {
		return added;
	}

	/** Beliefs lost during the execution. */
	public List<Literal> getDeleted() {
		return deleted;
	}

	public Outcome getOutcome() {
		return outcome;
	}

	/**
	 * A canonical key for the change in beliefs, so that identical changes can be grouped.
	 * @return
	 */
	public String deltaKey() {
		StringBuilder s = new StringBuilder();
		for (Literal l: deleted) {
			s.append("-").append(l).append(",");
		}
		for (Literal l: added) {
			s.append("+").append(l).append(",");
		}
		return s.toString();
	}

	/**
	 * The change in beliefs as a post-condition: added beliefs as beliefs and deleted
	 * beliefs as negated beliefs.  An empty change gives the trivially true formula.
	 * @return
	 */
	public GLogicalFormula deltaAsPostcondition() {
		GLogicalFormula f = null;
		for (Literal l: added) {
			f = conj(f, new Guard(new GBelief(l.clone())));
		}
		for (Literal l: deleted) {
			f = conj(f, new Guard(Guard.GLogicalOp.not, new Guard(new GBelief(l.clone()))));
		}
		if (f == null) {
			return new GBelief();
		}
		return f;
	}

	private static GLogicalFormula conj(GLogicalFormula f, GLogicalFormula g) {
		if (f == null) {
			return g;
		}
		return new Guard(f, Guard.GLogicalOp.and, g);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (Literal l: deleted) {
			s.append(s.length() == 0 ? "" : ", ").append("-").append(l);
		}
		for (Literal l: added) {
			s.append(s.length() == 0 ? "" : ", ").append("+").append(l);
		}
		return action + " | " + s + " | " + outcome;
	}
}
