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
package ail.semantics.operationalrules;

import java.util.List;

import ail.semantics.AILAgent;
import ail.semantics.adaptive.ActionExecution;
import ail.semantics.adaptive.AdaptiveListener;
import ail.semantics.adaptive.AdaptiveReasoning;
import ail.semantics.adaptive.AdaptiveState;
import ail.syntax.Action;
import ail.syntax.ActionLog;
import ail.syntax.ActionLogEntry;
import ail.syntax.Deed;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.tracing.events.ActionEvent;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import gov.nasa.jpf.annotation.FilterField;

/**
 * Execute an action that has a durative description in the agent's capability library
 * (Stringer, Section 5.2).
 *
 * When the action is first reached it is executed in the environment and its intention is
 * suspended, marked as executing the action (Rule 5.1).  The intention is unsuspended once a
 * terminating condition of the action is believed (see {@link CheckExecutingActions}) and this
 * rule then resolves the outcome, recording it in the action log with the change in the agent's
 * beliefs:
 * <ul>
 * <li>success: the action is removed from the intention (Rules 5.3 and 5.4);</li>
 * <li>failure: the action remains on the intention and is attempted again (Rules 5.5 and 5.6),
 *     after returning to a state satisfying its pre-condition if necessary,
 *     unless it has reached its failure threshold, in which case it is deprecated and the
 *     reasoning cycle moves on to learn a new description for it and reconfigure the agent's plans;</li>
 * <li>abort: the action is aborted in the environment if it is still running and is not attempted
 *     again (Rules 5.7 and 5.8).  The rest of the plan depended on the action, so the intention's goal
 *     fails in the same way as for any other failed action in the AIL.</li>
 * </ul>
 * Instantaneous actions follow the same route: their outcome is judged once the agent has
 * perceived their effects.
 *
 * @author Peter Stringer
 */
public class HandleDurativeAction extends HandleTopDeed {
	@FilterField
	private static final String name = "Handle Durative Action";

	@FilterField
	private Action act;
	@FilterField
	private AdaptiveReasoning.Description description;

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.operationalrules.HandleTopDeed#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.operationalrules.HandleTopDeed#checkPreconditions(ail.semantics.AILAgent)
	 */
	public boolean checkPreconditions(AILAgent a) {
		if (! super.checkPreconditions(a) || topdeed.getCategory() != Deed.DAction) {
			return false;
		}
		if (a.getAdaptiveState().getExecution(i.getID()) != null) {
			return true;
		}
		Action content = (Action) topdeed.getContent();
		if (content.getActionType() != Action.normalAction) {
			return false;
		}
		Unifier u = thetahd.clone();
		u.compose(thetab);
		act = (Action) content.clone();
		act.apply(u);
		description = AdaptiveReasoning.describe(a, act);
		return description != null;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.operationalrules.HandleTopDeed#apply(ail.semantics.AILAgent)
	 */
	public void apply(AILAgent a) {
		ActionExecution e = a.getAdaptiveState().getExecution(i.getID());
		if (e == null) {
			start(a);
		} else {
			resolve(a, e);
		}
	}

	/**
	 * Execute the action and suspend the intention until it terminates.
	 */
	private void start(AILAgent a) {
		Unifier result = null;
		ActionExecution e = new ActionExecution(act, description.entry, description.instance, AdaptiveReasoning.beliefs(a), AdaptiveReasoning.now(a), null);
		try {
			result = a.getEnv().executeAction(a.getAgName(), act);
			e = new ActionExecution(act, description.entry, description.instance, e.getBeliefsBefore(), e.getStartTime(), result);
		} catch (AILexception ex) {
			// The environment could not execute the action.  Unless the environment reports it as
			// executing it will be judged to have failed once the agent's beliefs are up to date.
			AJPFLogger.fine(AdaptiveReasoning.logname, a.getAgName() + " could not execute " + act + ": " + ex.getMessage());
		}
		if (a.shouldTrace()) {
			a.trace(new ActionEvent(act, i_id));
		}
		a.getAdaptiveState().startExecution(i.getID(), e);
		for (AdaptiveListener l: a.getAdaptiveState().getListeners()) {
			l.actionStarted(a, act, description.instance);
		}
		i.suspend();
		a.getReasoningCycle().setStopandCheck(true);
	}

	/**
	 * Resolve the outcome of an action whose intention has been unsuspended.
	 */
	private void resolve(AILAgent a, ActionExecution e) {
		ActionLogEntry.Outcome o = AdaptiveReasoning.outcome(a, e);
		if (o == null) {
			// Still executing.
			i.suspend();
			return;
		}

		AdaptiveState s = a.getAdaptiveState();
		ActionLog log = s.getActionLog();
		s.endExecution(i.getID());
		ActionLogEntry entry = new ActionLogEntry(e.getAction(), e.getBeliefsBefore(), AdaptiveReasoning.beliefs(a), o);
		log.add(entry);
		if (AJPFLogger.ltInfo(AdaptiveReasoning.logname)) {
			AJPFLogger.info(AdaptiveReasoning.logname, a.getAgName() + " action log: " + entry);
		}
		for (AdaptiveListener l: s.getListeners()) {
			l.actionTerminated(a, entry);
		}

		Predicate action = e.getAction();
		switch (o) {
		case SUCCESS:
			i.tlI(a);
			thetahd.compose(thetab);
			if (e.getResult() != null) {
				thetahd.compose(e.getResult());
			}
			i.compose(thetahd);
			break;
		case ABORT:
			abortInEnvironment(a, e);
			thetahd.compose(thetab);
			AdaptiveReasoning.failTopDeed(a, i, thetahd);
			break;
		case FAILURE:
			int threshold = e.getDescription().getThreshold();
			if (log.failureCount(action) >= threshold) {
				AJPFLogger.info(AdaptiveReasoning.logname, a.getAgName() + ": " + action + " is deprecated after " + log.failureCount(action) + " failure(s)");
				s.setPendingDeprecation(new AdaptiveState.Deprecation(i.getID(), action, e.getLibraryEntry(), e.getDescription()));
				for (AdaptiveListener l: s.getListeners()) {
					l.actionDeprecated(a, action, log.failureCount(action));
				}
			} else {
				retry(a, e);
			}
			break;
		}
	}

	/**
	 * Attempt a failed action again (Rules 5.5 and 5.6): the action remains the top deed.  If its
	 * pre-condition no longer holds the agent first returns to a state where it does, using its
	 * other actions; if it cannot, the action has failed and so does the intention's goal.
	 */
	private void retry(AILAgent a, ActionExecution e) {
		if (AdaptiveReasoning.believes(a, e.getDescription().getPre())) {
			return;
		}
		List<Predicate> back = AdaptiveReasoning.planFromBeliefs(a, e.getDescription().getPre());
		if (back == null) {
			AJPFLogger.warning(AdaptiveReasoning.logname, a.getAgName() + " cannot re-establish the pre-condition of " + e.getAction() + " to retry it");
			thetahd.compose(thetab);
			AdaptiveReasoning.failTopDeed(a, i, thetahd);
		} else {
			AJPFLogger.info(AdaptiveReasoning.logname, a.getAgName() + " returns with " + back + " to retry " + e.getAction());
			AdaptiveReasoning.pushActions(i, back);
		}
	}

	/**
	 * Abort an action in the environment if it is still running there.
	 */
	private static void abortInEnvironment(AILAgent a, ActionExecution e) {
		if (a.getEnv().executing(a.getAgName(), e.getAction())) {
			Predicate abort = new Predicate("abort");
			abort.addTerm(new Predicate(e.getAction()));
			try {
				a.getEnv().executeAction(a.getAgName(), new Action(abort, Action.normalAction));
			} catch (AILexception ex) {
				AJPFLogger.warning(AdaptiveReasoning.logname, a.getAgName() + " could not abort " + e.getAction() + ": " + ex.getMessage());
			}
		}
	}
}
