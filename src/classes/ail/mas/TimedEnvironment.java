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
package ail.mas;

/**
 * An environment that keeps time, so that actions with durations can time out.
 *
 * Time is kept by the environment rather than read from the system clock so that runs are
 * repeatable and can be model checked.  An environment that advances its clock while an
 * action is executing should wake its agents, since a timeout need not change any percepts.
 *
 * @author Peter Stringer
 */
public interface TimedEnvironment {
	/**
	 * The current time, in the same units as action durations.
	 * @return
	 */
	public long getTime();
}
