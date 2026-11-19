// ----------------------------------------------------------------------------
// Copyright (C) 2026 Peter Stringer, Louise A. Dennis
//
// This file is part of the Gwendolen Interpreter.
//
// The Gwendolen Interpreter is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// The Gwendolen Interpreter is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with the Gwendolen Interpreter; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------
package gwendolen.adaptive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ail.mas.AIL;
import ail.mas.MAS;
import ail.semantics.AILAgent;
import ail.syntax.ActionLogEntry;
import ail.syntax.Capability;
import ail.syntax.Deed;
import ail.syntax.DurativeAction;
import ail.syntax.Plan;
import ail.syntax.ast.GroundPredSets;
import ail.util.AILConfig;
import ajpf.MCAPLcontroller;

/**
 * End to end tests of agents that detect deprecated actions, learn new descriptions of them and
 * patch their plans (Stringer, Chapters 5 and 6), using the scenarios of Chapter 7.
 */
public class AdaptiveAgentQuickTests {
	private static final String PATROL = "/src/examples/gwendolen/adaptive/patrol.ail";
	private static final String THRESHOLD = "/src/tests/gwendolen/adaptive/patrol_threshold.ail";
	private static final String INSPECTION = "/src/examples/gwendolen/adaptive/inspection.ail";

	/** Run a program to completion with some configuration overridden. */
	private static MAS run(String file, String... overrides) {
		GroundPredSets.clear();
		AILConfig config = new AILConfig(file);
		config.setProperty("suppress_version", "true");
		config.remove("log.info");
		for (int k = 0; k < overrides.length; k += 2) {
			if (overrides[k + 1] == null) {
				config.remove(overrides[k]);
			} else {
				config.setProperty(overrides[k], overrides[k + 1]);
			}
		}
		MCAPLcontroller control = new MCAPLcontroller(config, "");
		MAS mas = AIL.AILSetup(config, control);
		control.begin();
		mas.cleanup();
		return mas;
	}

	private static List<String> outcomes(AILAgent ag, ActionLogEntry.Outcome o) {
		ArrayList<String> out = new ArrayList<String>();
		for (ActionLogEntry e: ag.getAdaptiveState().getActionLog().getEntries()) {
			if (e.getOutcome() == o) {
				out.add(e.getAction().toString());
			}
		}
		return out;
	}

	private static String route(AILAgent ag) {
		for (Plan p: ag.getPL().getPlans()) {
			if (p.getTriggerEvent().toString().contains("route")) {
				ArrayList<String> ds = new ArrayList<String>();
				for (Deed d: p.getBody()) {
					ds.add(d.getContent().toString());
				}
				Collections.reverse(ds);
				return ds.toString();
			}
		}
		return null;
	}

	private static DurativeAction learned(AILAgent ag) {
		for (Capability c: ag.getCL()) {
			if (c instanceof DurativeAction && ((DurativeAction) c).isLearned()) {
				return (DurativeAction) c;
			}
		}
		return null;
	}

	/** With no faults the agent behaves as an ordinary Gwendolen agent. */
	@Test public void patrolWithoutFaults() {
		MAS mas = run(PATROL, "waypoints.fault.move(b,d)", null);
		AILAgent rover = mas.getAg("rover");
		Assert.assertEquals(Arrays.asList("a", "b", "d", "c", "b", "a", "b", "d", "c", "b", "a"), ((WaypointsEnv) mas.getEnv()).getJourney());
		Assert.assertTrue(outcomes(rover, ActionLogEntry.Outcome.FAILURE).isEmpty());
		Assert.assertEquals(10, outcomes(rover, ActionLogEntry.Outcome.SUCCESS).size());
		Assert.assertNull(learned(rover));
		Assert.assertEquals("[move(a,b), move(b,d), move(d,c), move(c,b), move(b,a)]", route(rover));
	}

	/**
	 * Section 7.3.1: move(b,d) takes the rover to c.  The failure is detected, a new description of
	 * move(b,d) is learned and the route is patched (Equation 7.4).  The current patrol is repaired
	 * and the second patrol follows the patched route without failing.
	 */
	@Test public void patrolLearnsAndPatchesRoute() {
		MAS mas = run(PATROL);
		AILAgent rover = mas.getAg("rover");
		Assert.assertEquals(Arrays.asList("a", "b", "c", "d", "c", "b", "a", "b", "c", "d", "c", "b", "a"), ((WaypointsEnv) mas.getEnv()).getJourney());
		Assert.assertEquals(Arrays.asList("move(b,d)"), outcomes(rover, ActionLogEntry.Outcome.FAILURE));
		Assert.assertEquals("{at(b)} move(b,d) {at(c) & ~at(b)}", learned(rover).toString());
		Assert.assertEquals("[move(a,b), move(b,d), move(c,d), move(d,c), move(c,b), move(b,a)]", route(rover));
		Assert.assertFalse(rover.getAdaptiveState().hasPendingDeprecation());
		Assert.assertFalse(rover.getAdaptiveState().hasExecutingActions());
	}

	/**
	 * With a threshold of 2 a failed move is retried after the rover returns to where it started,
	 * and is only deprecated after its second failure.
	 */
	@Test public void failuresBelowThresholdAreRetried() {
		MAS mas = run(THRESHOLD);
		AILAgent rover = mas.getAg("rover");
		Assert.assertEquals(Arrays.asList("a", "b", "c", "b", "c", "d", "c", "b", "a"), ((WaypointsEnv) mas.getEnv()).getJourney());
		Assert.assertEquals(Arrays.asList("move(b,d)", "move(b,d)"), outcomes(rover, ActionLogEntry.Outcome.FAILURE));
		Assert.assertTrue(learned(rover).toString().startsWith("{at(b)} move(b,d) {at(c) & ~at(b)}"));
		Assert.assertEquals("[move(a,b), move(b,d), move(c,d), move(d,c), move(c,b), move(b,a)]", route(rover));
	}

	/** Section 5.3: moves take time and the robot's intention waits for them to finish. */
	@Test public void durativeActionsSuspendTheirIntention() {
		MAS mas = run(INSPECTION);
		HallwayEnv env = (HallwayEnv) mas.getEnv();
		AILAgent inspector = mas.getAg("inspector");
		Assert.assertEquals(Arrays.asList("room4", "hallway", "picture_location", "hallway", "room4"), env.getJourney());
		Assert.assertEquals(1, env.getPictures());
		// Four moves and the picture; print has no description so is not monitored.
		Assert.assertEquals(5, outcomes(inspector, ActionLogEntry.Outcome.SUCCESS).size());
		// Each move took the configured travel time of 2 ticks, so the run spanned at least 8 ticks.
		Assert.assertTrue(env.getTime() >= 8);
	}

	/**
	 * A move that never arrives is aborted when it exceeds its duration.  It is not attempted again
	 * and the inspection fails, so the robot never photographs the exit.
	 */
	@Test public void overrunningActionsAreAborted() {
		MAS mas = run(INSPECTION, "hallway.fault.move(hallway,picture_location)", "never");
		HallwayEnv env = (HallwayEnv) mas.getEnv();
		AILAgent inspector = mas.getAg("inspector");
		Assert.assertEquals(Arrays.asList("move(hallway,picture_location)"), env.getAborted());
		Assert.assertEquals(Arrays.asList("move(hallway,picture_location)"), outcomes(inspector, ActionLogEntry.Outcome.ABORT));
		Assert.assertEquals(0, env.getPictures());
		Assert.assertEquals(Arrays.asList("room4", "hallway", "hallway"), env.getJourney());
		Assert.assertTrue(inspector.getIntentions().isEmpty() && (inspector.getIntention() == null || inspector.getIntention().empty()));
	}

	/**
	 * The only way to the picture location now leads to room1, so no patch exists: the new
	 * description is still learned, but the plan is unchanged and the inspection fails.
	 */
	@Test public void unpatchableActionFailsTheGoal() {
		MAS mas = run(INSPECTION, "hallway.fault.move(hallway,picture_location)", "room1");
		HallwayEnv env = (HallwayEnv) mas.getEnv();
		AILAgent inspector = mas.getAg("inspector");
		Assert.assertEquals(Arrays.asList("move(hallway,picture_location)"), outcomes(inspector, ActionLogEntry.Outcome.FAILURE));
		Assert.assertTrue(learned(inspector).toString().startsWith("{at(hallway)} move(hallway,picture_location) {at(room1) & ~at(hallway)}"));
		Assert.assertTrue(route(inspector).startsWith("[move(room4,hallway), move(hallway,picture_location), take_picture"));
		Assert.assertEquals(0, env.getPictures());
	}
}
