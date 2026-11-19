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

import java.util.List;
import java.util.Map;

import ail.semantics.AILAgent;
import ail.syntax.Action;
import ail.syntax.ActionLogEntry;
import ail.syntax.DurativeAction;
import ail.syntax.Predicate;

/**
 * Observes an agent monitoring and adapting its actions, e.g. to visualise or record it.
 * All methods do nothing by default.
 *
 * @author Peter Stringer
 */
public interface AdaptiveListener {
	/** An action with a durative description has been started and its intention suspended. */
	default void actionStarted(AILAgent a, Action action, DurativeAction description) {}

	/** An action has terminated and its outcome has been added to the action log. */
	default void actionTerminated(AILAgent a, ActionLogEntry entry) {}

	/** An action has reached its failure threshold. */
	default void actionDeprecated(AILAgent a, Predicate action, int failures) {}

	/** A new description has been learned (Stage D1), with the score of each candidate change in beliefs. */
	default void descriptionLearned(AILAgent a, DurativeAction learned, Map<String, Integer> scores) {}

	/** Plans have been reconfigured (Stage D2).  The patch is null if none was found. */
	default void plansReconfigured(AILAgent a, Predicate action, List<Predicate> patch, int plansChanged) {}

	/** The intention executing a deprecated action has been repaired, or failed if the repair is null. */
	default void intentionRepaired(AILAgent a, Predicate action, List<Predicate> repair) {}
}
