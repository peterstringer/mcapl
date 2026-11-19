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
import java.util.Iterator;
import java.util.List;

import ail.mas.TimedEnvironment;
import ail.semantics.AILAgent;
import ail.syntax.Action;
import ail.syntax.ActionLogEntry;
import ail.syntax.Capability;
import ail.syntax.CapabilityLibrary;
import ail.syntax.Deed;
import ail.syntax.DurativeAction;
import ail.syntax.Event;
import ail.syntax.GBelief;
import ail.syntax.GLogicalFormula;
import ail.syntax.Goal;
import ail.syntax.Guard;
import ail.syntax.GuardAtom;
import ail.syntax.Intention;
import ail.syntax.Literal;
import ail.syntax.Predicate;
import ail.syntax.Unifier;

/**
 * Reasoning about actions with terminating conditions, shared by the operational rules for
 * executing durative actions, learning new action descriptions and reconfiguring plans.
 *
 * @author Peter Stringer
 */
public final class AdaptiveReasoning {
	/** Logger name for action outcomes, deprecations, learned descriptions and plan patches. */
	public static final String logname = "ail.semantics.adaptive";

	private AdaptiveReasoning() {}

	/**
	 * A description of an action: the entry in the capability library and that entry
	 * instantiated for the action.
	 */
	public static final class Description {
		public final DurativeAction entry;
		public final DurativeAction instance;

		Description(DurativeAction e, DurativeAction i) {
			entry = e;
			instance = i;
		}
	}

	/**
	 * Find the description of an action in the agent's capability library.  Learned descriptions of
	 * specific actions are added to the front of the library so they take precedence over the more
	 * general descriptions they replace.
	 * @param a
	 * @param act a ground action.
	 * @return the description or null if the action has no durative description.
	 */
	public static Description describe(AILAgent a, Predicate act) {
		CapabilityLibrary cl = a.getCL();
		if (cl == null || ! act.isGround()) {
			return null;
		}
		Iterator<Capability> cs = cl.getRelevant(act, AILAgent.SelectionOrder.LINEAR);
		while (cs.hasNext()) {
			Capability c = cs.next();
			if (c instanceof DurativeAction) {
				DurativeAction d = ((DurativeAction) c).clone();
				Unifier u = new Unifier();
				if (d.getCap().unifies(act, u)) {
					d.apply(u);
					return new Description((DurativeAction) c, d);
				}
			}
		}
		return null;
	}

	/**
	 * Does the agent believe a formula?
	 * @param a
	 * @param f
	 * @return false if the formula is null.
	 */
	public static boolean believes(AILAgent a, GLogicalFormula f) {
		if (f == null) {
			return false;
		}
		Guard g;
		if (f instanceof Guard) {
			g = (Guard) f;
		} else if (f instanceof GuardAtom) {
			g = new Guard((GuardAtom<?>) f);
		} else {
			g = new Guard(f, Guard.GLogicalOp.and, new Guard());
		}
		return a.believesyn(g, new Unifier());
	}

	/**
	 * A snapshot of the agent's beliefs.
	 * @param a
	 * @return
	 */
	public static ArrayList<Literal> beliefs(AILAgent a) {
		return new ArrayList<Literal>(a.getBB().getAll());
	}

	/**
	 * Have the agent's beliefs caught up with its perceptions?  Changes in perception become
	 * intentions to add or remove beliefs and an action's outcome should only be judged once
	 * these have been processed.
	 * @param a
	 * @return
	 */
	public static boolean beliefsSettled(AILAgent a) {
		if (pendingPercept(a.getIntention())) {
			return false;
		}
		for (Intention i: a.getIntentions()) {
			if (pendingPercept(i)) {
				return false;
			}
		}
		return true;
	}

	private static boolean pendingPercept(Intention i) {
		return i != null && ! i.empty() && i.hdE().fromPercept();
	}

	/**
	 * The environment's time, or -1 if it keeps no time.
	 * @param a
	 * @return
	 */
	public static long now(AILAgent a) {
		if (a.getEnv() instanceof TimedEnvironment) {
			return ((TimedEnvironment) a.getEnv()).getTime();
		}
		return -1;
	}

	/**
	 * Has an action run for longer than its duration?
	 * @param a
	 * @param e
	 * @return
	 */
	public static boolean timedOut(AILAgent a, ActionExecution e) {
		int d = e.getDescription().getDuration();
		long t = now(a);
		return d > 0 && t >= 0 && e.getStartTime() >= 0 && t - e.getStartTime() > d;
	}

	/**
	 * How, if at all, an executing action has terminated.  Success is checked first, then abort
	 * (an explicit abort condition, or a timeout while the environment is still executing the
	 * action), then failure.  An action the environment reports
	 * as no longer executing that has neither succeeded nor been aborted has failed.
	 * @param a
	 * @param e
	 * @return the outcome, or null if the action is still executing.
	 */
	public static ActionLogEntry.Outcome outcome(AILAgent a, ActionExecution e) {
		DurativeAction d = e.getDescription();
		if (believes(a, d.getSuccess())) {
			return ActionLogEntry.Outcome.SUCCESS;
		}
		boolean executing = a.getEnv().executing(a.getAgName(), e.getAction());
		// Only an action that is still running can overrun its duration: one that has finished
		// may just be waiting for the agent to process its effects.
		if (believes(a, d.getAbort()) || (executing && timedOut(a, e))) {
			return ActionLogEntry.Outcome.ABORT;
		}
		if (believes(a, d.getFailure()) || ! executing) {
			return ActionLogEntry.Outcome.FAILURE;
		}
		return null;
	}

	/**
	 * The capability library as STRIPS operators.  Capabilities outside the STRIPS fragment are omitted.
	 * @param cl
	 * @return
	 */
	public static List<StripsPlanner.Operator> operators(CapabilityLibrary cl) {
		ArrayList<StripsPlanner.Operator> ops = new ArrayList<StripsPlanner.Operator>();
		for (Capability c: cl) {
			boolean override = c instanceof DurativeAction && ((DurativeAction) c).isLearned() && c.getCap().isGround();
			StripsPlanner.Operator op = StripsPlanner.Operator.fromCapability(c, override);
			if (op != null) {
				ops.add(op);
			}
		}
		return ops;
	}

	/**
	 * Find a patch for plans containing a deprecated action (Cardoso et al. 2019): a sequence of
	 * actions that achieves what the action was originally intended to achieve, starting from a
	 * state in which only the action's original pre-conditions are known to hold.
	 * @param a
	 * @param d the description of the deprecated action, instantiated for the action.
	 * @return the patch, or null if none was found.
	 */
	public static List<Predicate> findPatch(AILAgent a, DurativeAction d) {
		StripsConditions pre = StripsConditions.fromFormula(d.getOriginalPre());
		StripsConditions goal = StripsConditions.fromFormula(d.getOriginalPost());
		if (pre == null || goal == null) {
			return null;
		}
		return a.getAdaptiveState().getPlanner().plan(pre.getPositive(), goal, operators(a.getCL()));
	}

	/**
	 * Find a sequence of actions that achieves what a deprecated action was originally intended
	 * to achieve, starting from the agent's current beliefs.  This is used to repair the intention
	 * that was executing the action when it was deprecated.
	 * @param a
	 * @param d the description of the deprecated action, instantiated for the action.
	 * @return the actions, or null if none were found.
	 */
	public static List<Predicate> planFromBeliefs(AILAgent a, DurativeAction d) {
		return planFromBeliefs(a, d.getOriginalPost());
	}

	/**
	 * Find a sequence of actions that makes a formula true, starting from the agent's current beliefs.
	 * @param a
	 * @param goal
	 * @return the actions, or null if none were found (or the formula is outside the STRIPS fragment).
	 */
	public static List<Predicate> planFromBeliefs(AILAgent a, GLogicalFormula goal) {
		StripsConditions g = StripsConditions.fromFormula(goal);
		if (g == null) {
			return null;
		}
		return a.getAdaptiveState().getPlanner().plan(beliefs(a), g, operators(a.getCL()));
	}

	/**
	 * Find one of the agent's intentions by its id.
	 * @param a
	 * @param id
	 * @return the intention, or null if the agent no longer has it.
	 */
	public static Intention findIntention(AILAgent a, int id) {
		if (a.getIntention() != null && a.getIntention().getID() == id) {
			return a.getIntention();
		}
		for (Intention i: a.getIntentions()) {
			if (i.getID() == id) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Treat the top deed of an intention as having failed.  If the intention is pursuing a goal, the
	 * goal's failure event (-!g) is raised, so that any plans for handling the failure apply, and the
	 * goal is then dropped together with the rest of the plan that depended on the deed.  Otherwise
	 * the deed is removed.
	 * @param a
	 * @param i
	 * @param theta the unifier for the top of the intention.
	 */
	public static void failTopDeed(AILAgent a, Intention i, Unifier theta) {
		Event e = i.hdE();
		if (e.referstoGoal()) {
			Goal goal = (Goal) e.getContent();
			i.iCons(e, new Deed(Deed.AILDeletion, goal), new Guard(new GBelief()), theta);
			i.iCons(new Event(Event.AILDeletion, goal), new Deed(Deed.Dnpy), new Guard(new GBelief()), theta.clone());
		} else {
			i.tlI(a);
			i.compose(theta);
		}
	}

	/**
	 * Replace the top deed of an intention with a sequence of actions, keeping the deed's event so
	 * that the intention continues to pursue the same goal.
	 * @param a
	 * @param i
	 * @param actions the actions in execution order.
	 */
	public static void replaceTopDeed(AILAgent a, Intention i, List<Predicate> actions) {
		if (actions.isEmpty()) {
			i.tlI(a);
			return;
		}
		Event e = i.hdE();
		Unifier theta = i.hdU();
		i.iReplace(e, new Deed(new Action(actions.get(actions.size() - 1), Action.normalAction)), new Guard(new GBelief()), theta);
		for (int k = actions.size() - 2; k >= 0; k--) {
			i.iCons(e, new Deed(new Action(actions.get(k), Action.normalAction)), new Guard(new GBelief()), theta.clone());
		}
	}

	/**
	 * Push a sequence of actions on top of an intention, ahead of its current top deed and pursuing
	 * the same event.
	 * @param i
	 * @param actions the actions in execution order.
	 */
	public static void pushActions(Intention i, List<Predicate> actions) {
		Event e = i.hdE();
		Unifier theta = i.hdU();
		for (int k = actions.size() - 1; k >= 0; k--) {
			i.iCons(e, new Deed(new Action(actions.get(k), Action.normalAction)), new Guard(new GBelief()), theta.clone());
		}
	}
}
