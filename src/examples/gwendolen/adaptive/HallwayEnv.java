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
import ail.mas.TimedEnvironment;
import ail.syntax.Action;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.util.AILConfig;
import ail.util.AILexception;
import ajpf.MCAPLJobber;
import ajpf.util.AJPFLogger;

/**
 * An inspection robot moving between rooms, a hallway and the location from which it photographs
 * the fire exit (Stringer, Sections 5.3 and 7.2.1).  Moves take time: move(From, To) takes the
 * robot out of From and it arrives some ticks later.  While it is in transit the action is still
 * executing.  Moves can be configured to go elsewhere or, with the destination "never", to leave
 * the robot stuck in transit until the move is aborted, after which it returns to where it started:
 *
 * <pre>
 * hallway.start = room4
 * hallway.travel = 2
 * hallway.fault.move(hallway,picture_location) = never
 * </pre>
 *
 * @author Peter Stringer
 */
public class HallwayEnv extends DefaultEnvironment implements TimedEnvironment, MCAPLJobber {
	static final String logname = "gwendolen.adaptive.HallwayEnv";
	static final String FAULT = "hallway.fault.";
	static final String NEVER = "never";

	private long time = 0;
	private int travel = 2;
	private String location = "room4";
	private final TreeMap<String, String> faults = new TreeMap<String, String>();
	private final ArrayList<String> journey = new ArrayList<String>();
	private final ArrayList<String> aborted = new ArrayList<String>();
	private int pictures = 0;

	// The move in progress, if any.
	private Predicate moving = null;
	private String origin;
	private String destination;
	private long arrival;

	public HallwayEnv() {
		super();
		getScheduler().addJobber(this);
	}

	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#configure(ail.util.AILConfig)
	 */
	@Override
	public void configure(AILConfig config) {
		super.configure(config);
		if (config.containsKey("hallway.start")) {
			location = config.getProperty("hallway.start").trim();
		}
		if (config.containsKey("hallway.travel")) {
			travel = Integer.parseInt(config.getProperty("hallway.travel").trim());
		}
		for (String key: config.stringPropertyNames()) {
			if (key.startsWith(FAULT)) {
				faults.put(key.substring(FAULT.length()).replace(" ", ""), config.getProperty(key).trim());
			}
		}
		journey.add(location);
		addPercept(at(location));
	}

	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#executeAction(java.lang.String, ail.syntax.Action)
	 */
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
		String a = new Predicate(act).toString();
		if (act.getFunctor().equals("move") && act.getTermsSize() == 2 && moving == null) {
			String from = act.getTerm(0).toString();
			if (from.equals(location)) {
				String to = act.getTerm(1).toString();
				moving = new Predicate(act);
				origin = from;
				synchronized (this) {
					destination = faults.containsKey(a) ? faults.get(a) : to;
				}
				arrival = time + travel;
				removePercept(at(location));
				location = null;
				AJPFLogger.info(logname, time + ": " + agName + " " + act + " leaves " + from);
			}
		} else if (act.getFunctor().equals("abort") && moving != null && act.getTerm(0).toString().equals(moving.toString())) {
			AJPFLogger.info(logname, time + ": " + agName + " aborts " + moving + ", returning to " + origin);
			aborted.add(moving.toString());
			arrive(origin);
		} else if (act.getFunctor().equals("take_picture") && "picture_location".equals(location)) {
			pictures++;
			AJPFLogger.info(logname, time + ": " + agName + " photographs the fire exit");
		}
		return super.executeAction(agName, act);
	}

	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#executing(java.lang.String, ail.syntax.Action)
	 */
	@Override
	public boolean executing(String agName, Action act) {
		return moving != null && moving.toString().equals(new Predicate(act).toString());
	}

	/**
	 * Advance the clock and complete any move that is due.  Agents are woken on every tick while
	 * a move is in progress since a timeout need not change their perceptions.
	 */
	@Override
	public void do_job() {
		time++;
		if (moving != null && ! NEVER.equals(destination) && time >= arrival) {
			AJPFLogger.info(logname, time + ": arrives at " + destination);
			arrive(destination);
		}
		notifyListeners();
	}

	private void arrive(String w) {
		moving = null;
		location = w;
		journey.add(w);
		addPercept(at(w));
	}

	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#done()
	 */
	@Override
	public boolean done() {
		return moving == null;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.mas.TimedEnvironment#getTime()
	 */
	@Override
	public long getTime() {
		return time;
	}

	private static Predicate at(String w) {
		Predicate p = new Predicate("at");
		p.addTerm(new Predicate(w));
		return p;
	}

	public List<String> getJourney() {
		return Collections.unmodifiableList(journey);
	}

	public List<String> getAborted() {
		return Collections.unmodifiableList(aborted);
	}

	public int getPictures() {
		return pictures;
	}

	/**
	 * Make move(From, To) take the robot elsewhere, or with "never" leave it stuck in transit.
	 */
	public synchronized void setFault(String from, String to, String actual) {
		faults.put("move(" + from + "," + to + ")", actual);
	}

	public synchronized void clearFault(String from, String to) {
		faults.remove("move(" + from + "," + to + ")");
	}

	public synchronized TreeMap<String, String> getFaults() {
		return new TreeMap<String, String>(faults);
	}

	/** Where the robot is, or null while it is moving. */
	public String getLocation() {
		return location;
	}

	/** The move in progress, or null. */
	public Predicate getMove() {
		return moving;
	}

	/** Where the move in progress started. */
	public String getOrigin() {
		return origin;
	}

	/** Where the move in progress will end, or "never". */
	public String getDestination() {
		return destination;
	}

	/** When the move in progress will arrive. */
	public long getArrival() {
		return arrival;
	}

	public int getTravel() {
		return travel;
	}

	/*
	 * (non-Javadoc)
	 * @see ajpf.MCAPLJobber#getName()
	 */
	@Override
	public String getName() {
		return "gwendolen.adaptive.HallwayEnv";
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MCAPLJobber o) {
		return getName().compareTo(o.getName());
	}
}
