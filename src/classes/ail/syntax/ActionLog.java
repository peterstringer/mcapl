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
package ail.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A bounded log of action executions, oldest first.  When the log is full the oldest entry is
 * discarded so that the log reflects recent behaviour of the agent's actions.
 *
 * @author Peter Stringer
 */
public class ActionLog {
	/** The size of an action log if none is configured. */
	public static final int DEFAULT_SIZE = 50;

	/** The status of an action, derived from its recent failures (Stringer et al. 2020). */
	public static enum Status { ACTIVE, SUSPECT, DEPRECATED }

	private final ArrayList<ActionLogEntry> entries = new ArrayList<ActionLogEntry>();
	private int sizelimit;

	public ActionLog() {
		this(DEFAULT_SIZE);
	}

	public ActionLog(int size) {
		setSizeLimit(size);
	}

	/**
	 * Change the maximum number of entries.  Excess entries are discarded, oldest first.
	 * @param size
	 */
	public void setSizeLimit(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("Action log size must be positive: " + size);
		}
		sizelimit = size;
		trim();
	}

	public int getSizeLimit() {
		return sizelimit;
	}

	/**
	 * Append an entry, discarding the oldest entry if the log is full.
	 * @param e
	 */
	public void add(ActionLogEntry e) {
		entries.add(e);
		trim();
	}

	private void trim() {
		while (entries.size() > sizelimit) {
			entries.remove(0);
		}
	}

	/**
	 * All entries, oldest first.
	 * @return
	 */
	public List<ActionLogEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public int size() {
		return entries.size();
	}

	/**
	 * The most recent entry.
	 * @return the latest entry or null if the log is empty.
	 */
	public ActionLogEntry getLatestEntry() {
		if (entries.isEmpty()) {
			return null;
		}
		return entries.get(entries.size() - 1);
	}

	/**
	 * The failures of an action that have not yet been used to learn a new description, oldest first.
	 * @param action a ground action.
	 * @return
	 */
	public List<ActionLogEntry> getUnconsumedFailures(Predicate action) {
		ArrayList<ActionLogEntry> fs = new ArrayList<ActionLogEntry>();
		for (ActionLogEntry e: entries) {
			if (! e.consumed && e.getOutcome() == ActionLogEntry.Outcome.FAILURE && e.getAction().equals(action)) {
				fs.add(e);
			}
		}
		return fs;
	}

	/**
	 * The number of failures of an action since a description was last learned for it.
	 * @param action a ground action.
	 * @return
	 */
	public int failureCount(Predicate action) {
		return getUnconsumedFailures(action).size();
	}

	/**
	 * Reset the failure count of an action, once a new description has been learned for it.
	 * @param action
	 */
	public void resetFailures(Predicate action) {
		for (ActionLogEntry e: getUnconsumedFailures(action)) {
			e.consumed = true;
		}
	}

	/**
	 * The status of an action given its failure threshold.  An action is suspect if it has failed
	 * but not yet reached its threshold, and deprecated once it has.
	 * @param action a ground action.
	 * @param threshold
	 * @return
	 */
	public Status status(Predicate action, int threshold) {
		int n = failureCount(action);
		if (n >= threshold) {
			return Status.DEPRECATED;
		} else if (n > 0) {
			return Status.SUSPECT;
		}
		return Status.ACTIVE;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (ActionLogEntry e: entries) {
			s.append(e).append("\n");
		}
		return s.toString();
	}
}
