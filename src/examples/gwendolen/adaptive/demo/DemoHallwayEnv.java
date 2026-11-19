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
package gwendolen.adaptive.demo;

import ail.syntax.Action;
import ail.syntax.Unifier;
import ail.util.AILexception;
import gwendolen.adaptive.HallwayEnv;

/**
 * The hallway, with a clock that ticks in real time and reports the robot's progress to the demo page.
 *
 * @author Peter Stringer
 */
public class DemoHallwayEnv extends HallwayEnv {
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
		DemoServer demo = DemoServer.get();
		boolean wasMoving = getMove() != null;
		Unifier u = super.executeAction(agName, act);
		if (act.getFunctor().equals("move") && getMove() != null && ! wasMoving) {
			demo.publish("depart", "action", act.toString(), "from", getOrigin(), "intended", act.getTerm(1).toString(),
					"actual", getDestination(), "time", getTime(), "arrival", getArrival());
		} else if (act.getFunctor().equals("abort")) {
			demo.publish("envabort", "action", act.getTerm(0).toString(), "at", getLocation(), "time", getTime());
		} else if (act.getFunctor().equals("take_picture")) {
			demo.publish("picture", "pictures", getPictures(), "at", getLocation());
		} else if (act.getFunctor().equals("print")) {
			demo.publish("print", "text", act.getTerm(0).toString());
		}
		return u;
	}

	@Override
	public void do_job() {
		DemoServer demo = DemoServer.get();
		if (! demo.pause(demo.tickMs)) {
			return;
		}
		boolean wasMoving = getMove() != null;
		super.do_job();
		demo.publish("tick", "time", getTime(), "at", getLocation(), "moving", getMove() == null ? null : getMove().toString(),
				"origin", getOrigin(), "destination", getDestination(), "arrival", getArrival(), "arrived", wasMoving && getMove() == null);
	}
}
