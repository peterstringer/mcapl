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

import java.util.HashMap;
import java.util.List;

import ail.syntax.ActionLogEntry;

/**
 * Synthesises a new post-condition for a deprecated action from its failures in the action log
 * (Stringer, Algorithm 2).
 *
 * Each failure is weighted by its recency: the oldest failure scores 1, the next 2 and so on.
 * The weights of failures with identical changes in belief are summed and the change with the
 * highest total becomes the new post-condition.  This combines frequency and recency so that a
 * consistent recent change in behaviour outweighs older or one-off outcomes.  When every failure
 * produced the same change that change is trivially selected.  Ties are broken in favour of the
 * change that was observed most recently.
 *
 * @author Peter Stringer
 */
public final class PostconditionLearner {

	private PostconditionLearner() {}

	/**
	 * Select the representative failure for a new post-condition.
	 * @param failures the failures of a single action, oldest first.
	 * @return the most recent entry with the highest scoring change in beliefs, or null if there are no failures.
	 */
	public static ActionLogEntry select(List<ActionLogEntry> failures) {
		HashMap<String, Integer> scores = scores(failures);
		ActionLogEntry best = null;
		int bestscore = 0;
		// Iterate newest first so that ties go to the most recently observed change,
		// and the entry returned is the most recent with that change.
		for (int i = failures.size() - 1; i >= 0; i--) {
			int s = scores.get(failures.get(i).deltaKey());
			if (s > bestscore) {
				bestscore = s;
				best = failures.get(i);
			}
		}
		return best;
	}

	/**
	 * The score given to each distinct change in belief, for reporting.
	 * @param failures the failures of a single action, oldest first.
	 * @return
	 */
	public static HashMap<String, Integer> scores(List<ActionLogEntry> failures) {
		HashMap<String, Integer> scores = new HashMap<String, Integer>();
		int n = 1;
		for (ActionLogEntry e: failures) {
			String k = e.deltaKey();
			Integer s = scores.get(k);
			scores.put(k, (s == null ? 0 : s) + n);
			n++;
		}
		return scores;
	}
}
