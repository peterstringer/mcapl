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
package ail.semantics.adaptive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ail.syntax.ActionLogEntry;
import ail.syntax.GBelief;
import ail.syntax.GLogicalFormula;
import ail.syntax.Guard;
import ail.syntax.Literal;

/**
 * A conjunction of literals that should be believed (positive) and literals that should not be
 * believed (negative).  This is the fragment of guard formulas that can be read as STRIPS
 * preconditions, goals and effects (Fikes and Nilsson 1971): as a post-condition the positive
 * literals are the add list and the negative literals the delete list.
 *
 * @author Peter Stringer
 */
public final class StripsConditions {
	private final List<Literal> positive;
	private final List<Literal> negative;

	public StripsConditions(List<Literal> pos, List<Literal> neg) {
		positive = Collections.unmodifiableList(new ArrayList<Literal>(pos));
		negative = Collections.unmodifiableList(new ArrayList<Literal>(neg));
	}

	public List<Literal> getPositive() {
		return positive;
	}

	public List<Literal> getNegative() {
		return negative;
	}

	/**
	 * Read a formula as a conjunction of believed and not-believed literals.
	 * @param f
	 * @return the conditions or null if the formula is outside the STRIPS fragment
	 *  (e.g., it contains a disjunction or an equation).
	 */
	public static StripsConditions fromFormula(GLogicalFormula f) {
		ArrayList<Literal> pos = new ArrayList<Literal>();
		ArrayList<Literal> neg = new ArrayList<Literal>();
		if (collect(f, true, pos, neg)) {
			return new StripsConditions(pos, neg);
		}
		return null;
	}

	private static boolean collect(GLogicalFormula f, boolean positive, List<Literal> pos, List<Literal> neg) {
		if (f == null) {
			return true;
		}
		if (f instanceof GBelief) {
			GBelief b = (GBelief) f;
			if (b.isTrivial()) {
				// Trivially true when positive, unsatisfiable when negated.
				return positive;
			}
			Literal l = ActionLogEntry.stripAnnotations(b);
			if (positive) {
				pos.add(l);
			} else {
				neg.add(l);
			}
			return true;
		}
		if (f instanceof Guard) {
			Guard g = (Guard) f;
			switch (g.getOp()) {
			case none:
				if (g.getLHS() != null && ! collect(g.getLHS(), positive, pos, neg)) {
					return false;
				}
				return collect(g.getRHS(), positive, pos, neg);
			case and:
				if (! positive) {
					// A negated conjunction is a disjunction.
					return false;
				}
				return collect(g.getLHS(), true, pos, neg) && collect(g.getRHS(), true, pos, neg);
			case not:
				return collect(g.getRHS(), ! positive, pos, neg);
			default:
				return false;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (Literal l: positive) {
			s.append(s.length() == 0 ? "" : ", ").append(l);
		}
		for (Literal l: negative) {
			s.append(s.length() == 0 ? "" : ", ").append("~").append(l);
		}
		return "{" + s + "}";
	}
}
