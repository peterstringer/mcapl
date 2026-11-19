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
import static ail.adaptive.AdaptiveTestSupport.capabilities;
import static ail.adaptive.AdaptiveTestSupport.capability;
import static ail.adaptive.AdaptiveTestSupport.lit;
import static ail.adaptive.AdaptiveTestSupport.lits;
import static ail.adaptive.AdaptiveTestSupport.moved;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ail.semantics.adaptive.AdaptiveReasoning;
import ail.semantics.adaptive.AdaptiveState;
import ail.semantics.adaptive.StripsConditions;
import ail.semantics.adaptive.StripsPlanner;
import ail.syntax.Action;
import ail.syntax.ActionLogEntry.Outcome;
import ail.syntax.CapabilityLibrary;
import ail.syntax.Deed;
import ail.syntax.DurativeAction;
import ail.syntax.Event;
import ail.syntax.GBelief;
import ail.syntax.Guard;
import ail.syntax.Literal;
import ail.syntax.Plan;
import ail.syntax.PlanLibrary;
import ail.syntax.Predicate;

/**
 * Tests of plan reconfiguration with learned action descriptions (Stringer, Section 6.3.2).
 */
public class ReconfigurationQuickTests {

	/** The patrol capabilities of Figure 6.4, with explicit delete effects. */
	private static final String PATROL =
			  "{at(1)} move(1,2) {at(2), ~at(1)}\n"
			+ "{at(2)} move(2,3) {at(3), ~at(2)}\n"
			+ "{at(3)} move(3,0) {at(0), ~at(3)}\n"
			+ "{at(3)} move(3,1) {at(1), ~at(3)}\n";

	private static CapabilityLibrary library(String text) {
		CapabilityLibrary cl = new CapabilityLibrary();
		for (DurativeAction d: capabilities(text)) {
			cl.add(d);
		}
		return cl;
	}

	/** Learn a description of move(0,1) from a failure that left the agent at the given waypoint. */
	private static DurativeAction learnMove01(String to) {
		DurativeAction move01 = capability("{at(0)} move(0,1) {at(1), ~at(0)}");
		return move01.withLearnedPost(moved(act("move", "0", "1"), "0", to, Outcome.FAILURE).deltaAsPostcondition());
	}

	private static List<Predicate> patch(CapabilityLibrary cl, DurativeAction learned) {
		cl.addFirst(learned);
		StripsConditions pre = StripsConditions.fromFormula(learned.getOriginalPre());
		StripsConditions goal = StripsConditions.fromFormula(learned.getOriginalPost());
		return new StripsPlanner().plan(pre.getPositive(), goal, AdaptiveReasoning.operators(cl));
	}

	@Test public void learnedDescriptionKeepsIntendedEffect() {
		DurativeAction learned = learnMove01("3");
		Assert.assertTrue(learned.isLearned());
		Assert.assertEquals("[at(3)]", StripsConditions.fromFormula(learned.getPost()).getPositive().toString());
		Assert.assertEquals("[at(1)]", StripsConditions.fromFormula(learned.getOriginalPost()).getPositive().toString());
		Assert.assertEquals(learned.getPost(), learned.getSuccess());
	}

	/** move(0,1) now leads to waypoint 3, so the patch is move(0,1), move(3,1). */
	@Test public void patchUsesTheLearnedDescription() {
		Assert.assertEquals("[move(0,1), move(3,1)]", patch(library(PATROL), learnMove01("3")).toString());
	}

	/** move(0,1) no longer moves the agent: there is no patch unless another route exists. */
	@Test public void patchFindsAnotherRoute() {
		Assert.assertNull(patch(library(PATROL), learnMove01("0")));

		String more = PATROL + "{at(0)} move(0,2) {at(2), ~at(0)}\n{at(2)} move(2,1) {at(1), ~at(2)}\n";
		Assert.assertEquals("[move(0,2), move(2,1)]", patch(library(more), learnMove01("0")).toString());
	}

	/** A learned description of move(0,1) overrides a general description of move. */
	@Test public void learnedDescriptionOverridesGeneralOne() {
		CapabilityLibrary cl = library("{at(X), link(X,Y)} move(X,Y) {at(Y), ~at(X)}");
		DurativeAction general = (DurativeAction) cl.iterator().next().clone();
		general.apply(unifierFor(general, act("move", "0", "1")));
		cl.addFirst(general.withLearnedPost(moved(act("move", "0", "1"), "0", "0", Outcome.FAILURE).deltaAsPostcondition()));

		List<Literal> init = lits(lit("at", "0"), lit("link", "0", "1"), lit("link", "0", "2"), lit("link", "2", "1"));
		StripsConditions goal = StripsConditions.fromFormula(general.getOriginalPost());
		List<Predicate> plan = new StripsPlanner().plan(init, goal, AdaptiveReasoning.operators(cl));
		Assert.assertEquals("[move(0,2), move(2,1)]", plan.toString());
	}

	private static ail.syntax.Unifier unifierFor(DurativeAction d, Action a) {
		ail.syntax.Unifier u = new ail.syntax.Unifier();
		Assert.assertTrue(d.getCap().unifies(a, u));
		return u;
	}

	@Test public void satisfiedGoalNeedsNoActions() {
		StripsConditions goal = StripsConditions.fromFormula(capability("{at(0)} move(0,1) {at(1), ~at(0)}").getOriginalPost());
		List<Predicate> plan = new StripsPlanner().plan(lits(lit("at", "1")), goal, AdaptiveReasoning.operators(library(PATROL)));
		Assert.assertTrue(plan.isEmpty());
	}

	// ----- Patching plan bodies.  Bodies are stacks: the last deed is executed first.

	private static ArrayList<Deed> body(Action... actions) {
		ArrayList<Deed> ds = new ArrayList<Deed>();
		for (Action a: actions) {
			ds.add(new Deed(a));
		}
		Collections.reverse(ds);
		return ds;
	}

	private static String inOrder(List<Deed> body) {
		ArrayList<String> out = new ArrayList<String>();
		for (Deed d: body) {
			out.add(d.getContent().toString());
		}
		Collections.reverse(out);
		return out.toString();
	}

	private static List<Predicate> seq(Action... as) {
		return new ArrayList<Predicate>(Arrays.asList(as));
	}

	/** Equations 7.1 and 7.4: move(b,d) now leads to c. */
	@Test public void routeIsPatched() {
		AdaptiveState s = new AdaptiveState();
		s.setPatch(act("move", "b", "d"), seq(act("move", "b", "d"), act("move", "c", "d")));
		ArrayList<Deed> route = body(act("move", "a", "b"), act("move", "b", "d"), act("move", "d", "c"), act("move", "c", "b"), act("move", "b", "a"));
		Assert.assertEquals("[move(a,b), move(b,d), move(c,d), move(d,c), move(c,b), move(b,a)]", inOrder(s.patchBody(route)));
	}

	@Test public void repatchingReplacesTheOldPatch() {
		AdaptiveState s = new AdaptiveState();
		PlanLibrary pl = new PlanLibrary();
		Plan route = new Plan(new Event(Event.AILAddition, Event.AILBel, new Literal("route")), new ArrayList<Deed>(), new ArrayList<Guard>(),
				body(act("move", "a", "b"), act("move", "b", "d"), act("move", "d", "c")));
		Guard atA = new Guard(new GBelief(lit("at", "a")));
		route.setContextSingle(atA, 3);
		pl.add(route);

		s.setPatch(act("move", "b", "d"), seq(act("move", "b", "d"), act("move", "c", "d")));
		Assert.assertEquals(1, s.reconfigure(pl));
		Plan patched = pl.getPlans().get(0);
		Assert.assertEquals("[move(a,b), move(b,d), move(c,d), move(d,c)]", inOrder(patched.getBody()));
		// The context stays parallel to the body and the plan's guard stays with its first deed.
		Assert.assertEquals(patched.getBody().size(), patched.getContext().size());
		Assert.assertSame(atA, patched.getContext().get(patched.getContext().size() - 1));

		s.setPatch(act("move", "b", "d"), seq(act("move", "b", "a"), act("move", "a", "d")));
		s.reconfigure(pl);
		Assert.assertEquals("[move(a,b), move(b,a), move(a,d), move(d,c)]", inOrder(pl.getPlans().get(0).getBody()));
	}

	@Test public void patchesAreAppliedWithinPatches() {
		AdaptiveState s = new AdaptiveState();
		s.setPatch(act("move", "b", "d"), seq(act("move", "b", "d"), act("move", "c", "d")));
		s.setPatch(act("move", "c", "d"), seq(act("move", "c", "a"), act("move", "a", "d")));
		ArrayList<Deed> route = body(act("move", "a", "b"), act("move", "b", "d"), act("move", "d", "c"));
		Assert.assertEquals("[move(a,b), move(b,d), move(c,a), move(a,d), move(d,c)]", inOrder(s.patchBody(route)));
	}
}
