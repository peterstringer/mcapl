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
package gwendolen.parser;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ail.mas.MAS;
import ail.semantics.AILAgent;
import ail.semantics.adaptive.StripsConditions;
import ail.syntax.Capability;
import ail.syntax.DurativeAction;
import ail.syntax.ast.Abstract_DurativeAction;
import gwendolen.GwendolenMASBuilder;

/**
 * Tests of the :Capabilities: section of Gwendolen programs.
 */
public class CapabilityParsingQuickTests {

	private static DurativeAction parse(String s) {
		List<Abstract_DurativeAction> as = GwendolenCapabilityParser.parse(s);
		Assert.assertEquals(1, as.size());
		return as.get(0).toMCAPL();
	}

	@Test public void plainCapability() {
		DurativeAction d = parse("{at(a)} move(a,b) {at(b), ~at(a)}");
		Assert.assertEquals("move(a,b)", d.getCap().toString());
		Assert.assertEquals("[at(a)]", StripsConditions.fromFormula(d.getPre()).getPositive().toString());
		StripsConditions post = StripsConditions.fromFormula(d.getPost());
		Assert.assertEquals("[at(b)]", post.getPositive().toString());
		Assert.assertEquals("[at(a)]", post.getNegative().toString());
		Assert.assertSame(d.getPost(), d.getSuccess());
		Assert.assertNull(d.getFailure());
		Assert.assertNull(d.getAbort());
		Assert.assertEquals(0, d.getDuration());
		Assert.assertEquals(DurativeAction.DEFAULT_THRESHOLD, d.getThreshold());
	}

	/** The example of Section 5.3, with a failure threshold. */
	@Test public void terminatingConditionsDurationAndThreshold() {
		DurativeAction d = parse("{at(hallway)} move(hallway, fire_exit) {at(fire_exit), ~at(hallway)}\n"
				+ "    [success {at(fire_exit)}, failure {at(entrance)}, abort {blocked, ~safe}, duration 300, threshold 4]");
		Assert.assertEquals("[at(fire_exit)]", StripsConditions.fromFormula(d.getSuccess()).getPositive().toString());
		Assert.assertEquals("[at(entrance)]", StripsConditions.fromFormula(d.getFailure()).getPositive().toString());
		StripsConditions abort = StripsConditions.fromFormula(d.getAbort());
		Assert.assertEquals("[blocked]", abort.getPositive().toString());
		Assert.assertEquals("[safe]", abort.getNegative().toString());
		Assert.assertEquals(300, d.getDuration());
		Assert.assertEquals(4, d.getThreshold());
	}

	@Test public void emptyConditionsAndVariables() {
		DurativeAction d = parse("{} take_picture {}");
		Assert.assertTrue(StripsConditions.fromFormula(d.getPre()).getPositive().isEmpty());
		DurativeAction m = parse("{at(X), link(X,Y)} move(X,Y) {at(Y), ~at(X)}");
		Assert.assertFalse(m.getCap().isGround());
	}

	@Test public void malformedCapabilitiesAreReported() {
		String[] bad = { "{at(a)} move(a,b)", "{at(a)} {at(b)}", "{at(a)} move(a,b) {at(b)} [speed 3]", "{at(a)} move(a,b) {at(b)} [threshold 0]" };
		for (String s: bad) {
			try {
				GwendolenCapabilityParser.parse(s);
				Assert.fail("Parsed " + s);
			} catch (IllegalArgumentException e) {
				Assert.assertTrue(e.getMessage().startsWith("Error in :Capabilities:"));
			}
		}
	}

	/** Capabilities in a program reach the agent's capability library. */
	@Test public void programCapabilities() {
		String program = "GWENDOLEN\n:name: rover\n:Initial Beliefs:\nat(a)\n"
				+ ":Capabilities:\n"
				+ "// comments are allowed\n"
				+ "{at(a)} move(a,b) {at(b), ~at(a)} [threshold 3]\n"
				+ "/* across\n lines */ {at(b)} move(b,a) {at(a), ~at(b)}\n"
				+ ":Initial Goals:\nroute [perform]\n"
				+ ":Plans:\n+!route [perform] : {B at(a)} <- move(a,b), move(b,a);\n";
		MAS mas = new GwendolenMASBuilder(program, false).getMAS();
		AILAgent ag = mas.getAg("rover");
		ArrayList<String> caps = new ArrayList<String>();
		for (Capability c: ag.getCL()) {
			Assert.assertTrue(c instanceof DurativeAction);
			caps.add(c.getCap() + "/" + ((DurativeAction) c).getThreshold());
		}
		Assert.assertEquals("[move(a,b)/3, move(b,a)/1]", caps.toString());
		Assert.assertEquals(1, ag.getPL().size());
	}
}
