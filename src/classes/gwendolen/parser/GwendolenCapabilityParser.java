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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import ail.parser.FOFVisitor;
import ail.syntax.Literal;
import ail.syntax.ast.Abstract_DurativeAction;
import ail.syntax.ast.Abstract_GBelief;
import ail.syntax.ast.Abstract_GLogicalFormula;
import ail.syntax.ast.Abstract_Guard;
import ail.syntax.ast.Abstract_Literal;
import ail.syntax.ast.Abstract_Pred;
import ail.syntax.ast.Abstract_Predicate;
import ajpf.psl.parser.LogicalFmlasLexer;
import ajpf.psl.parser.LogicalFmlasParser;

/**
 * Parses the :Capabilities: section of a Gwendolen agent.  Each capability is an action with
 * pre- and post-conditions, optionally followed by terminating conditions, a duration and a
 * failure threshold:
 *
 * <pre>
 * {at(0)} move(0, 1) {at(1), ~at(0)}
 * {at(hallway)} move(hallway, exit) {at(exit), ~at(hallway)} [failure {at(entrance)}, abort {blocked}, duration 300, threshold 4]
 * </pre>
 *
 * Conditions are comma separated literals.  In a pre-condition (or terminating condition) ~l means
 * l is not believed; in a post-condition it means l stops being believed.
 *
 * @author Peter Stringer
 */
public final class GwendolenCapabilityParser {
	private final String text;
	private int pos = 0;
	private final FOFVisitor fofvisitor = new FOFVisitor();

	private GwendolenCapabilityParser(String s) {
		text = s;
	}

	/**
	 * Parse the text of a :Capabilities: section.
	 * @param s
	 * @return the capabilities in the order they were written.
	 * @throws IllegalArgumentException if the text is not a list of capabilities.
	 */
	public static List<Abstract_DurativeAction> parse(String s) {
		return new GwendolenCapabilityParser(s).capabilities();
	}

	private List<Abstract_DurativeAction> capabilities() {
		ArrayList<Abstract_DurativeAction> caps = new ArrayList<Abstract_DurativeAction>();
		skipWhitespace();
		while (pos < text.length()) {
			caps.add(capability());
			skipWhitespace();
		}
		return caps;
	}

	private Abstract_DurativeAction capability() {
		String pre = bracketed('{', '}');
		int start = pos;
		while (pos < text.length() && text.charAt(pos) != '{') {
			if (text.charAt(pos) == '(') {
				bracketed('(', ')');
			} else {
				pos++;
			}
		}
		String action = text.substring(start, pos).trim();
		if (action.isEmpty()) {
			throw error("expected an action");
		}
		String post = bracketed('{', '}');

		LogicalFmlasParser p = fofparser(action);
		Abstract_DurativeAction cap = new Abstract_DurativeAction((Abstract_Predicate) fofvisitor.visitPred(p.pred()));
		cap.addPre(conditions(pre));
		cap.addPost(conditions(post));

		skipWhitespace();
		if (pos < text.length() && text.charAt(pos) == '[') {
			for (String annotation: splitTopLevel(bracketed('[', ']'))) {
				annotate(cap, annotation);
			}
		}
		return cap;
	}

	private void annotate(Abstract_DurativeAction cap, String annotation) {
		String[] kv = annotation.split("\\s+", 2);
		String key = kv[0];
		String value = kv.length > 1 ? kv[1].trim() : "";
		switch (key) {
		case "success":
			cap.setSuccess(conditions(braced(value)));
			break;
		case "failure":
			cap.setFailure(conditions(braced(value)));
			break;
		case "abort":
			cap.setAbort(conditions(braced(value)));
			break;
		case "duration":
			cap.setDuration(number(key, value));
			break;
		case "threshold":
			int t = number(key, value);
			if (t < 1) {
				throw error("threshold must be at least 1");
			}
			cap.setThreshold(t);
			break;
		default:
			throw error("unknown capability annotation '" + key + "'");
		}
	}

	private int number(String key, String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw error(key + " should be a whole number, not '" + value + "'");
		}
	}

	private String braced(String value) {
		if (! value.startsWith("{") || ! value.endsWith("}")) {
			throw error("expected a condition in braces, not '" + value + "'");
		}
		return value.substring(1, value.length() - 1);
	}

	/**
	 * A conjunction of literals as a guard: ~l means l is not believed.
	 */
	private Abstract_GLogicalFormula conditions(String s) {
		Abstract_Guard g = new Abstract_Guard();
		boolean empty = true;
		for (String item: splitTopLevel(s)) {
			Abstract_Literal l = (Abstract_Literal) fofvisitor.visitLiteral(fofparser(item).literal());
			if (l.negated()) {
				g.add(new Abstract_GBelief(new Abstract_Literal(Literal.LPos, new Abstract_Pred(l))), false);
			} else {
				g.add(new Abstract_GBelief(l), true);
			}
			empty = false;
		}
		if (empty) {
			return new Abstract_GBelief();
		}
		return g;
	}

	/**
	 * Read a bracketed string starting at the current position, returning its contents.
	 */
	private String bracketed(char open, char close) {
		skipWhitespace();
		if (pos >= text.length() || text.charAt(pos) != open) {
			throw error("expected '" + open + "'");
		}
		int depth = 0;
		int start = pos + 1;
		while (pos < text.length()) {
			char c = text.charAt(pos);
			if (c == open) {
				depth++;
			} else if (c == close) {
				depth--;
				if (depth == 0) {
					pos++;
					return text.substring(start, pos - 1);
				}
			}
			pos++;
		}
		throw error("missing '" + close + "'");
	}

	/**
	 * Split a string at commas that are not nested inside brackets.
	 */
	private static List<String> splitTopLevel(String s) {
		ArrayList<String> items = new ArrayList<String>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '(' || c == '{' || c == '[') {
				depth++;
			} else if (c == ')' || c == '}' || c == ']') {
				depth--;
			} else if (c == ',' && depth == 0) {
				items.add(s.substring(start, i).trim());
				start = i + 1;
			}
		}
		String last = s.substring(start).trim();
		if (! last.isEmpty() || ! items.isEmpty()) {
			items.add(last);
		}
		return items;
	}

	private void skipWhitespace() {
		while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
			pos++;
		}
	}

	private IllegalArgumentException error(String msg) {
		int line = 1;
		for (int i = 0; i < pos && i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				line++;
			}
		}
		return new IllegalArgumentException("Error in :Capabilities: (line " + line + " of section): " + msg);
	}

	private static LogicalFmlasParser fofparser(String s) {
		LogicalFmlasLexer lexer = new LogicalFmlasLexer(CharStreams.fromString(s));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		return new LogicalFmlasParser(tokens);
	}
}
