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

import java.util.ArrayList;
import java.util.List;

import ail.syntax.Action;
import ail.syntax.ActionLogEntry;
import ail.syntax.DurativeAction;
import ail.syntax.Literal;
import ail.syntax.NumberTermImpl;
import ail.syntax.Predicate;
import ail.syntax.Term;
import ail.syntax.ast.Abstract_DurativeAction;
import gwendolen.parser.GwendolenCapabilityParser;

/**
 * Helpers for building the examples from the thesis in tests.
 */
final class AdaptiveTestSupport {
	private AdaptiveTestSupport() {}

	/** A literal such as at(0) or at(b): numeric arguments become numbers, others constants. */
	static Literal lit(String functor, String... args) {
		Literal l = new Literal(functor);
		for (String a: args) {
			l.addTerm(term(a));
		}
		return l;
	}

	static Action act(String functor, String... args) {
		Action a = new Action(functor);
		for (String s: args) {
			a.addTerm(term(s));
		}
		return a;
	}

	private static Term term(String s) {
		if (s.matches("[0-9]+")) {
			return new NumberTermImpl(Integer.parseInt(s));
		}
		return new Predicate(s);
	}

	static List<Literal> lits(Literal... ls) {
		ArrayList<Literal> out = new ArrayList<Literal>();
		for (Literal l: ls) {
			out.add(l);
		}
		return out;
	}

	/** An action log entry for an execution that moved the agent from one waypoint to another. */
	static ActionLogEntry moved(Predicate action, String from, String to, ActionLogEntry.Outcome o) {
		return new ActionLogEntry(action, lits(lit("at", from)), lits(lit("at", to)), o);
	}

	/** Parse capabilities written in Gwendolen's :Capabilities: syntax. */
	static List<DurativeAction> capabilities(String text) {
		ArrayList<DurativeAction> out = new ArrayList<DurativeAction>();
		for (Abstract_DurativeAction a: GwendolenCapabilityParser.parse(text)) {
			out.add(a.toMCAPL());
		}
		return out;
	}

	static DurativeAction capability(String text) {
		return capabilities(text).get(0);
	}
}
