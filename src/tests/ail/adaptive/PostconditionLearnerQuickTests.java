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
import static ail.adaptive.AdaptiveTestSupport.moved;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ail.semantics.adaptive.PostconditionLearner;
import ail.syntax.Action;
import ail.syntax.ActionLogEntry;
import ail.syntax.ActionLogEntry.Outcome;

/**
 * Tests of post-condition synthesis (Stringer, Algorithm 2).
 */
public class PostconditionLearnerQuickTests {
	private static final Action M01 = act("move", "0", "1");

	private static ActionLogEntry toWaypoint3() {
		return moved(M01, "0", "3", Outcome.FAILURE);
	}

	private static ActionLogEntry noChange() {
		return moved(M01, "0", "0", Outcome.FAILURE);
	}

	/** Figure 6.1: every failure took the agent to waypoint 3. */
	@Test public void consistentFailuresGiveTheirChange() {
		List<ActionLogEntry> fs = new ArrayList<ActionLogEntry>();
		for (int i = 0; i < 4; i++) {
			fs.add(toWaypoint3());
		}
		Assert.assertEquals("-at(0),+at(3),", PostconditionLearner.select(fs).deltaKey());
	}

	/** Figures 6.2 and 6.3: {-at(0), +at(3)} scores 1 + 2 = 3, {} scores 3 + 4 = 7. */
	@Test public void recentFrequentChangeIsSelected() {
		List<ActionLogEntry> fs = new ArrayList<ActionLogEntry>();
		fs.add(toWaypoint3());
		fs.add(toWaypoint3());
		fs.add(noChange());
		fs.add(noChange());
		Assert.assertEquals(Integer.valueOf(3), PostconditionLearner.scores(fs).get("-at(0),+at(3),"));
		Assert.assertEquals(Integer.valueOf(7), PostconditionLearner.scores(fs).get(""));
		ActionLogEntry best = PostconditionLearner.select(fs);
		Assert.assertTrue(best.getAdded().isEmpty() && best.getDeleted().isEmpty());
		Assert.assertSame(fs.get(3), best);
	}

	/** Frequency can outweigh recency: 1 + 3 = 4 beats 2. */
	@Test public void frequentChangeCanOutweighAMoreRecentOne() {
		List<ActionLogEntry> fs = new ArrayList<ActionLogEntry>();
		fs.add(toWaypoint3());
		fs.add(noChange());
		fs.add(toWaypoint3());
		Assert.assertEquals("-at(0),+at(3),", PostconditionLearner.select(fs).deltaKey());
	}

	/** 1 + 2 = 3 ties with 3: the most recently observed change wins. */
	@Test public void tiesGoToTheMostRecentChange() {
		List<ActionLogEntry> fs = new ArrayList<ActionLogEntry>();
		fs.add(toWaypoint3());
		fs.add(toWaypoint3());
		fs.add(noChange());
		Assert.assertEquals("", PostconditionLearner.select(fs).deltaKey());
	}

	@Test public void noFailuresGiveNothing() {
		Assert.assertNull(PostconditionLearner.select(new ArrayList<ActionLogEntry>()));
	}
}
