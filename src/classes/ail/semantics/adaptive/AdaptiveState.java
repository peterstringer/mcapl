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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ail.syntax.Action;
import ail.syntax.ActionLog;
import ail.syntax.Deed;
import ail.syntax.DurativeAction;
import ail.syntax.GBelief;
import ail.syntax.Guard;
import ail.syntax.Plan;
import ail.syntax.PlanLibrary;
import ail.syntax.Predicate;

/**
 * The state an agent needs to monitor its actions, detect deprecated actions, learn new
 * descriptions for them and patch its plans: the action log, the actions currently executing,
 * any deprecated action awaiting learning and re-planning, and the patches made to the plan library.
 *
 * @author Peter Stringer
 */
public class AdaptiveState {
	private final ActionLog log = new ActionLog();

	/** Executing actions, indexed by the id of the (suspended) intention that started them. */
	private final HashMap<Integer, ActionExecution> executing = new HashMap<Integer, ActionExecution>();

	private Deprecation pending = null;

	/** Patches for deprecated actions, indexed by the printed ground action. */
	private final TreeMap<String, List<Predicate>> patches = new TreeMap<String, List<Predicate>>();

	/** Plans that have been patched, with their bodies and contexts before any patch was applied. */
	private final ArrayList<Plan> patchedPlans = new ArrayList<Plan>();
	private final ArrayList<ArrayList<Deed>> originalBodies = new ArrayList<ArrayList<Deed>>();
	private final ArrayList<ArrayList<Guard>> originalContexts = new ArrayList<ArrayList<Guard>>();

	private StripsPlanner planner = new StripsPlanner();

	/**
	 * An action that has reached its failure threshold and is awaiting a new description
	 * (Stage D1) and the reconfiguration of the agent's plans (Stage D2).
	 */
	public static final class Deprecation {
		private final int intention;
		private final Predicate action;
		private final DurativeAction libraryEntry;
		private final DurativeAction description;
		private DurativeAction learned = null;

		/**
		 * @param i the id of the intention that was executing the action.
		 * @param a the ground action.
		 * @param entry the description in the capability library that matched the action.
		 * @param d that description instantiated for the action.
		 */
		public Deprecation(int i, Predicate a, DurativeAction entry, DurativeAction d) {
			intention = i;
			action = a;
			libraryEntry = entry;
			description = d;
		}

		public int getIntentionID() {
			return intention;
		}

		public Predicate getAction() {
			return action;
		}

		public DurativeAction getLibraryEntry() {
			return libraryEntry;
		}

		public DurativeAction getDescription() {
			return description;
		}

		public DurativeAction getLearned() {
			return learned;
		}

		public void setLearned(DurativeAction d) {
			learned = d;
		}
	}

	public ActionLog getActionLog() {
		return log;
	}

	public StripsPlanner getPlanner() {
		return planner;
	}

	public void setPlanner(StripsPlanner p) {
		planner = p;
	}

	// ----- Executing actions

	public void startExecution(int intention, ActionExecution e) {
		executing.put(intention, e);
	}

	public ActionExecution getExecution(int intention) {
		return executing.get(intention);
	}

	public void endExecution(int intention) {
		executing.remove(intention);
	}

	public boolean hasExecutingActions() {
		return ! executing.isEmpty();
	}

	public Map<Integer, ActionExecution> getExecutions() {
		return Collections.unmodifiableMap(executing);
	}

	// ----- Deprecated actions

	public void setPendingDeprecation(Deprecation d) {
		pending = d;
	}

	public Deprecation getPendingDeprecation() {
		return pending;
	}

	public boolean hasPendingDeprecation() {
		return pending != null;
	}

	// ----- Plan patches

	/**
	 * Record the patch that replaces an action in the agent's plans.
	 * @param action the ground deprecated action.
	 * @param patch the actions that achieve what the action was intended to achieve.
	 */
	public void setPatch(Predicate action, List<Predicate> patch) {
		patches.put(key(action), new ArrayList<Predicate>(patch));
	}

	public List<Predicate> getPatch(Predicate action) {
		return patches.get(key(action));
	}

	public Map<String, List<Predicate>> getPatches() {
		return Collections.unmodifiableMap(patches);
	}

	/**
	 * Rewrite the plans in a plan library so that every ground occurrence of a patched action is
	 * replaced by its patch.  Plans are always rewritten from their original bodies, so an action that
	 * is patched a second time has its old patch replaced rather than patched again.  Patches are
	 * applied recursively: an action in a patch that has itself been patched is expanded, except for
	 * the patched action itself, which may appear in its own patch with its newly learned description.
	 *
	 * A plan's context (its stack of guards) runs parallel to its body.  The guard of a replaced deed
	 * moves to the first deed of its patch and the other deeds of the patch are guarded by True.
	 *
	 * @param pl
	 * @return the number of plans whose bodies changed.
	 */
	public int reconfigure(PlanLibrary pl) {
		int changed = 0;
		for (Plan p: pl.getPlans()) {
			int i = indexOfPlan(p);
			ArrayList<Deed> body = i < 0 ? p.getBody() : originalBodies.get(i);
			ArrayList<Guard> context = i < 0 ? p.getContext() : originalContexts.get(i);
			ArrayList<Deed> newbody = new ArrayList<Deed>();
			ArrayList<Guard> newcontext = new ArrayList<Guard>();
			patch(body, context, newbody, newcontext);
			if (! newbody.equals(p.getBody())) {
				if (i < 0) {
					patchedPlans.add(p);
					originalBodies.add(new ArrayList<Deed>(body));
					originalContexts.add(new ArrayList<Guard>(context));
				}
				p.setBody(newbody);
				p.setContext(newcontext);
				changed++;
			}
		}
		return changed;
	}

	/** Plans may be structurally equal, so patched plans are found by identity. */
	private int indexOfPlan(Plan p) {
		for (int i = 0; i < patchedPlans.size(); i++) {
			if (patchedPlans.get(i) == p) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Apply the patches to a plan body.  Plan bodies are stacks: the last deed is executed first.
	 * @param body
	 * @return
	 */
	public ArrayList<Deed> patchBody(List<Deed> body) {
		ArrayList<Guard> context = new ArrayList<Guard>();
		for (int k = 0; k < body.size(); k++) {
			context.add(new Guard(new GBelief()));
		}
		ArrayList<Deed> newbody = new ArrayList<Deed>();
		patch(body, context, newbody, new ArrayList<Guard>());
		return newbody;
	}

	/**
	 * Apply the patches to a plan body and its parallel context, both stacks.
	 */
	private void patch(List<Deed> body, List<Guard> context, List<Deed> newbody, List<Guard> newcontext) {
		ArrayList<Deed> deeds = new ArrayList<Deed>();
		ArrayList<Guard> guards = new ArrayList<Guard>();
		// Work in execution order.
		for (int k = body.size() - 1; k >= 0; k--) {
			ArrayList<Deed> expansion = expand(Collections.singletonList(body.get(k)), new HashSet<String>());
			for (int j = 0; j < expansion.size(); j++) {
				deeds.add(expansion.get(j));
				guards.add(j == 0 && k < context.size() ? context.get(k) : new Guard(new GBelief()));
			}
		}
		Collections.reverse(deeds);
		Collections.reverse(guards);
		newbody.addAll(deeds);
		newcontext.addAll(guards);
	}

	/**
	 * Replace patched actions in a sequence of deeds (in execution order).
	 */
	private ArrayList<Deed> expand(List<Deed> deeds, Set<String> expanding) {
		ArrayList<Deed> out = new ArrayList<Deed>();
		for (Deed d: deeds) {
			String k = patchedActionKey(d);
			if (k == null || expanding.contains(k)) {
				out.add(d);
				continue;
			}
			expanding.add(k);
			for (Predicate p: patches.get(k)) {
				Deed pd = new Deed(new Action(p, Action.normalAction));
				if (key(p).equals(k)) {
					out.add(pd);
				} else {
					out.addAll(expand(Collections.singletonList(pd), expanding));
				}
			}
			expanding.remove(k);
		}
		return out;
	}

	private String patchedActionKey(Deed d) {
		if (d.getCategory() != Deed.DAction || ! (d.getContent() instanceof Predicate)) {
			return null;
		}
		Predicate a = (Predicate) d.getContent();
		if (! a.isGround()) {
			return null;
		}
		String k = key(a);
		return patches.containsKey(k) ? k : null;
	}

	/**
	 * A key identifying a ground action independent of its action type.
	 * @param a
	 * @return
	 */
	public static String key(Predicate a) {
		return new Predicate(a).toString();
	}
}
