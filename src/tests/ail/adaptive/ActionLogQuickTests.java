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
package ail.adaptive;

import static ail.adaptive.AdaptiveTestSupport.act;
import static ail.adaptive.AdaptiveTestSupport.lit;
import static ail.adaptive.AdaptiveTestSupport.lits;
import static ail.adaptive.AdaptiveTestSupport.moved;

import org.junit.Assert;
import org.junit.Test;

import ail.semantics.adaptive.StripsConditions;
import ail.syntax.Action;
import ail.syntax.ActionLog;
import ail.syntax.ActionLogEntry;
import ail.syntax.ActionLogEntry.Outcome;

/**
 * Tests of the action log (Stringer, Sections 5.1 and 6.1).
 */
public class ActionLogQuickTests {

	@Test public void entryRecordsAdditionsAndDeletions() {
		ActionLogEntry e = moved(act("move", "0", "1"), "0", "3", Outcome.FAILURE);
		Assert.assertEquals("[at(3)]", e.getAdded().toString());
		Assert.assertEquals("[at(0)]", e.getDeleted().toString());
		Assert.assertEquals("move(0,1) | -at(0), +at(3) | FAILURE", e.toString());
	}

	@Test public void unchangedBeliefsAreNotPartOfTheChange() {
		ActionLogEntry e = new ActionLogEntry(act("move", "0", "1"),
				lits(lit("at", "0"), lit("battery", "ok")), lits(lit("battery", "ok"), lit("at", "0")), Outcome.FAILURE);
		Assert.assertTrue(e.getAdded().isEmpty());
		Assert.assertTrue(e.getDeleted().isEmpty());
	}

	@Test public void changeBecomesAStripsPostcondition() {
		ActionLogEntry e = moved(act("move", "0", "1"), "0", "3", Outcome.FAILURE);
		StripsConditions post = StripsConditions.fromFormula(e.deltaAsPostcondition());
		Assert.assertEquals("[at(3)]", post.getPositive().toString());
		Assert.assertEquals("[at(0)]", post.getNegative().toString());

		ActionLogEntry none = new ActionLogEntry(act("move", "0", "1"), lits(lit("at", "0")), lits(lit("at", "0")), Outcome.FAILURE);
		StripsConditions nopost = StripsConditions.fromFormula(none.deltaAsPostcondition());
		Assert.assertTrue(nopost.getPositive().isEmpty() && nopost.getNegative().isEmpty());
	}

	@Test public void logDiscardsOldestEntriesWhenFull() {
		ActionLog log = new ActionLog(3);
		for (int i = 0; i < 5; i++) {
			log.add(moved(act("move", "0", Integer.toString(i)), "0", Integer.toString(i), Outcome.SUCCESS));
		}
		Assert.assertEquals(3, log.size());
		Assert.assertEquals("move(0,2)", log.getEntries().get(0).getAction().toString());
		Assert.assertEquals("move(0,4)", log.getLatestEntry().getAction().toString());

		log.setSizeLimit(1);
		Assert.assertEquals(1, log.size());
		Assert.assertEquals("move(0,4)", log.getLatestEntry().getAction().toString());
	}

	/** Figure 6.1: one success then four failures of move(0,1) with a threshold of four. */
	@Test public void failuresAreCountedPerActionAndReset() {
		ActionLog log = new ActionLog();
		Action m01 = act("move", "0", "1");
		Action m12 = act("move", "1", "2");
		log.add(moved(m01, "0", "1", Outcome.SUCCESS));
		log.add(moved(m12, "1", "0", Outcome.FAILURE));
		for (int i = 0; i < 3; i++) {
			log.add(moved(m01, "0", "3", Outcome.FAILURE));
		}
		Assert.assertEquals(3, log.failureCount(act("move", "0", "1")));
		Assert.assertEquals(ActionLog.Status.SUSPECT, log.status(m01, 4));

		log.add(moved(m01, "0", "3", Outcome.FAILURE));
		Assert.assertEquals(ActionLog.Status.DEPRECATED, log.status(m01, 4));
		Assert.assertEquals(1, log.failureCount(m12));

		log.resetFailures(m01);
		Assert.assertEquals(0, log.failureCount(m01));
		Assert.assertEquals(ActionLog.Status.ACTIVE, log.status(m01, 4));
		Assert.assertEquals(1, log.failureCount(m12));
		// Resetting the count does not remove the history.
		Assert.assertEquals(6, log.size());
	}
}
