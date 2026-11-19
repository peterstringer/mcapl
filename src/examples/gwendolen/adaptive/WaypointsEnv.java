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
package gwendolen.adaptive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import ail.mas.DefaultEnvironment;
import ail.syntax.Action;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.util.AILConfig;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;

/**
 * A topological map of waypoints for a rover (Stringer, Section 7.2.1).  The rover perceives at(W)
 * for the waypoint it is at.  The action move(From, To) moves it from From to To, unless the
 * action has been configured to fail, in which case it ends up somewhere else.  Failures are
 * configured without the agent's knowledge, e.g.
 *
 * <pre>
 * waypoints.start = a
 * waypoints.fault.move(b,d) = c
 * </pre>
 *
 * The rover does not move if it is not at From when the action is executed.
 *
 * @author Peter Stringer
 */
public class WaypointsEnv extends DefaultEnvironment {
	static final String logname = "gwendolen.adaptive.WaypointsEnv";
	static final String FAULT = "waypoints.fault.";

	private String location = "a";
	private final TreeMap<String, String> faults = new TreeMap<String, String>();
	private final ArrayList<String> journey = new ArrayList<String>();

	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#configure(ail.util.AILConfig)
	 */
	@Override
	public void configure(AILConfig config) {
		super.configure(config);
		if (config.containsKey("waypoints.start")) {
			location = config.getProperty("waypoints.start").trim();
		}
		for (String key: config.stringPropertyNames()) {
			if (key.startsWith(FAULT)) {
				faults.put(key.substring(FAULT.length()).replace(" ", ""), config.getProperty(key).trim());
			}
		}
		journey.add(location);
		addPercept(at(location));
	}

	/**
	 * Make move(From, To) take the rover to another waypoint.
	 * @param from
	 * @param to
	 * @param actual
	 */
	public void setFault(String from, String to, String actual) {
		faults.put("move(" + from + "," + to + ")", actual);
	}

	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#executeAction(java.lang.String, ail.syntax.Action)
	 */
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
		if (act.getFunctor().equals("move") && act.getTermsSize() == 2) {
			String from = act.getTerm(0).toString();
			String to = act.getTerm(1).toString();
			if (from.equals(location)) {
				String dest = faults.containsKey(new Predicate(act).toString()) ? faults.get(new Predicate(act).toString()) : to;
				removePercept(at(location));
				location = dest;
				addPercept(at(location));
				journey.add(location);
			}
			AJPFLogger.info(logname, agName + " " + act + " -> at(" + location + ")");
		}
		return super.executeAction(agName, act);
	}

	private static Predicate at(String w) {
		Predicate p = new Predicate("at");
		p.addTerm(new Predicate(w));
		return p;
	}

	/**
	 * The waypoints the rover has been at, in order.
	 * @return
	 */
	public List<String> getJourney() {
		return Collections.unmodifiableList(journey);
	}

	public String getLocation() {
		return location;
	}
}
