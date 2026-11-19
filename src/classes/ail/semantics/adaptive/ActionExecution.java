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
import java.util.List;

import ail.syntax.Action;
import ail.syntax.DurativeAction;
import ail.syntax.Literal;
import ail.syntax.Unifier;

/**
 * An action that an agent has started executing and that has not yet terminated.  This is the
 * executing(a) marker of the semantics: the intention that started the action is suspended until
 * one of the action's terminating conditions is believed.
 *
 * @author Peter Stringer
 */
public final class ActionExecution {
	private final Action action;
	private final DurativeAction entry;
	private final DurativeAction description;
	private final ArrayList<Literal> beliefsBefore;
	private final long startTime;
	private final Unifier result;

	/**
	 * @param a the ground action.
	 * @param e the action's description in the capability library.
	 * @param d that description instantiated for this action.
	 * @param before the agent's beliefs when the action started.
	 * @param start the environment's time when the action started, or -1 if the environment keeps no time.
	 * @param r the unifier returned by the environment when the action was started.
	 */
	public ActionExecution(Action a, DurativeAction e, DurativeAction d, List<Literal> before, long start, Unifier r) {
		action = a;
		entry = e;
		description = d;
		beliefsBefore = new ArrayList<Literal>(before);
		startTime = start;
		result = r;
	}

	public Action getAction() {
		return action;
	}

	public DurativeAction getLibraryEntry() {
		return entry;
	}

	public Unifier getResult() {
		return result;
	}

	public DurativeAction getDescription() {
		return description;
	}

	public List<Literal> getBeliefsBefore() {
		return beliefsBefore;
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public String toString() {
		return "executing(" + action + ")";
	}
}
