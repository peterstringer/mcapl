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

import java.util.ArrayList;
import java.util.Map;

import ail.semantics.AILAgent;
import ail.semantics.OSRule;
import ail.semantics.adaptive.ActionExecution;
import ail.semantics.adaptive.AdaptiveReasoning;
import ail.syntax.Intention;
import gov.nasa.jpf.annotation.FilterField;

/**
 * Unsuspend intentions whose executing action has terminated: one of its success, failure or
 * abort conditions is believed, it has timed out or the environment has finished executing it.
 * Outcomes are only judged once the agent's beliefs have caught up with its perceptions.
 *
 * @author Peter Stringer
 */
public class CheckExecutingActions implements OSRule {
	@FilterField
	private static final String name = "Check Executing Actions";

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
		return a.getAdaptiveState().hasExecutingActions() && AdaptiveReasoning.beliefsSettled(a);
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.OSRule#apply(ail.semantics.AILAgent)
	 */
	public void apply(AILAgent a) {
		ArrayList<Integer> dropped = new ArrayList<Integer>();
		for (Map.Entry<Integer, ActionExecution> e: a.getAdaptiveState().getExecutions().entrySet()) {
			Intention i = AdaptiveReasoning.findIntention(a, e.getKey());
			if (i == null) {
				// The intention has been dropped, e.g. because its goal was achieved some other way.
				dropped.add(e.getKey());
			} else if (i.suspended() && AdaptiveReasoning.outcome(a, e.getValue()) != null) {
				i.unsuspend();
				a.tellawake();
			}
		}
		for (Integer id: dropped) {
			a.getAdaptiveState().endExecution(id);
		}
	}
}
