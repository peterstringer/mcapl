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
import ail.semantics.adaptive.AdaptiveListener;
import ail.semantics.adaptive.AdaptiveReasoning;
import ail.semantics.adaptive.AdaptiveState;
import ail.semantics.adaptive.PostconditionLearner;
import ail.syntax.ActionLog;
import ail.syntax.ActionLogEntry;
import ail.syntax.CapabilityLibrary;
import ail.syntax.DurativeAction;
import ajpf.util.AJPFLogger;
import gov.nasa.jpf.annotation.FilterField;

/**
 * Stage D1: learn a new description for a deprecated action from its failures in the action log
 * (Stringer, Section 6.3.1).
 *
 * The new post-condition is the change in beliefs selected by {@link PostconditionLearner}.  The
 * new description applies to the specific action that failed: it is added to the front of the
 * capability library so that it takes precedence over a more general description, and replaces any
 * earlier description of that specific action.  The action's failure count is then reset.
 *
 * @author Peter Stringer
 */
public class LearnActionDescription implements OSRule {
	@FilterField
	private static final String name = "Learn Action Description";

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
		return d != null && d.getLearned() == null;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.OSRule#apply(ail.semantics.AILAgent)
	 */
	public void apply(AILAgent a) {
		AdaptiveState.Deprecation d = a.getAdaptiveState().getPendingDeprecation();
		ActionLog log = a.getAdaptiveState().getActionLog();
		List<ActionLogEntry> failures = log.getUnconsumedFailures(d.getAction());
		ActionLogEntry best = PostconditionLearner.select(failures);

		DurativeAction learned = d.getDescription().withLearnedPost(best.deltaAsPostcondition());
		CapabilityLibrary cl = a.getCL();
		if (d.getLibraryEntry().getCap().isGround()) {
			// The old description was of this specific action, so it is replaced.
			cl.remove(d.getLibraryEntry());
		}
		cl.addFirst(learned);
		log.resetFailures(d.getAction());
		d.setLearned(learned);

		for (AdaptiveListener l: a.getAdaptiveState().getListeners()) {
			l.descriptionLearned(a, learned, PostconditionLearner.scores(failures));
		}
		AJPFLogger.info(AdaptiveReasoning.logname, a.getAgName() + " learned " + learned + " from " + failures.size() + " failure(s), scores " + PostconditionLearner.scores(failures));
	}
}
