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
import ail.semantics.OSRule;
import ail.semantics.adaptive.AdaptiveReasoning;
import ail.semantics.adaptive.AdaptiveState;
import ail.syntax.Action;
import ail.syntax.Deed;
import ail.syntax.Intention;
import ail.syntax.Predicate;
import ajpf.util.AJPFLogger;
import gov.nasa.jpf.annotation.FilterField;

/**
 * Stage D2: reconfigure the agent's plans once a new description has been learned for a
 * deprecated action (Stringer, Section 6.3.2, following Cardoso et al. 2019).
 *
 * A STRIPS planning problem is built whose initial state is the deprecated action's original
 * pre-condition and whose goal is its original post-condition, using every description in the
 * capability library (including the newly learned one).  If a plan is found it replaces the action
 * wherever it occurs in the plan library.
 *
 * The intention that was executing the action is also repaired: the action is replaced by a plan
 * from the agent's current beliefs to the action's original post-condition.  If there is no such
 * plan the action has failed and the intention's goal is dropped.
 *
 * @author Peter Stringer
 */
public class ReconfigurePlans implements OSRule {
	@FilterField
	private static final String name = "Reconfigure Plans";

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.OSRule#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.OSRule#checkPreconditions(ail.semantics.AILAgent)
	 */
	public boolean checkPreconditions(AILAgent a) {
		AdaptiveState.Deprecation d = a.getAdaptiveState().getPendingDeprecation();
		return d != null && d.getLearned() != null;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.OSRule#apply(ail.semantics.AILAgent)
	 */
	public void apply(AILAgent a) {
		AdaptiveState s = a.getAdaptiveState();
		AdaptiveState.Deprecation d = s.getPendingDeprecation();
		Predicate action = d.getAction();

		List<Predicate> patch = AdaptiveReasoning.findPatch(a, d.getDescription());
		if (patch != null) {
			s.setPatch(action, patch);
			int n = s.reconfigure(a.getPL());
			AJPFLogger.info(AdaptiveReasoning.logname, a.getAgName() + " patched " + n + " plan(s): " + action + " -> " + patch);
		} else {
			AJPFLogger.warning(AdaptiveReasoning.logname, a.getAgName() + " found no patch for " + action + "; plans containing it are unchanged");
		}

		Intention i = AdaptiveReasoning.findIntention(a, d.getIntentionID());
		if (i != null && ! i.empty() && executes(i, action)) {
			List<Predicate> repair = AdaptiveReasoning.planFromBeliefs(a, d.getDescription());
			if (repair != null) {
				AJPFLogger.info(AdaptiveReasoning.logname, a.getAgName() + " continues with " + repair + " in place of " + action);
				AdaptiveReasoning.replaceTopDeed(a, i, repair);
			} else {
				AJPFLogger.warning(AdaptiveReasoning.logname, a.getAgName() + " cannot achieve the intended effect of " + action + " from its current beliefs");
				AdaptiveReasoning.failTopDeed(a, i, i.hdU());
			}
		}

		s.setPendingDeprecation(null);
	}

	/**
	 * Is the top deed of the intention the deprecated action?
	 */
	private static boolean executes(Intention i, Predicate action) {
		Deed d = i.hdD();
		if (d.getCategory() != Deed.DAction) {
			return false;
		}
		Action top = (Action) d.getContent().clone();
		top.apply(i.hdU());
		return AdaptiveState.key(top).equals(AdaptiveState.key(action));
	}
}
