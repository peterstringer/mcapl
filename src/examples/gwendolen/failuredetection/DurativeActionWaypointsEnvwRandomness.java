/*
// ----------------------------------------------------------------------------
// Copyright (C) 2008-2012 Louise A. Dennis, Berndt Farwer, Michael Fisher and 
// Rafael H. Bordini.
// 
// This file is part of Gwendolen
//
// Gwendolen is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
// 
// Gwendolen is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with Gwendolen if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// 
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//----------------------------------------------------------------------------

package gwendolen.failuredetection;

import ail.mas.DefaultEnvironment;
import ail.mas.MAS;
import ail.semantics.AILAgent;
import ail.syntax.*;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import ajpf.util.choice.UniformBoolChoice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

*/
/**
 * A Simple Blocks' World Environment.
 *
 * @author louiseadennis
 *
 *//*

public class DurativeActionWaypointsEnvwRandomness extends DefaultEnvironment {
	static String logname = "gwendolen.failuredetection.DurativeActionEnv";

	// temporarily required for clock
	int seconds = 0;
	int failureCount = 1;
	protected boolean done = false;

	// set robot start co-ords to (0,0)
	double robot1_x = 0;
	double robot1_y = 0;

	UniformBoolChoice r;
	CapabilityLibrary capLibrary = new CapabilityLibrary();

	public DurativeActionWaypointsEnvwRandomness() {
		// Make environment
		super();
		AJPFLogger.fine(logname, "Environment Created");

		// Construct GBeliefs for grid
		Literal at00 = new Literal("at");
		at00.addTerm(new NumberTermImpl(0));
		at00.addTerm(new NumberTermImpl(0));

		Literal at01 = new Literal("at");
		at01.addTerm(new NumberTermImpl(0));
		at01.addTerm(new NumberTermImpl(1));

		Literal at02 = new Literal("at");
		at02.addTerm(new NumberTermImpl(0));
		at02.addTerm(new NumberTermImpl(2));

		Literal at03 = new Literal("at");
		at03.addTerm(new NumberTermImpl(0));
		at03.addTerm(new NumberTermImpl(3));

		Literal at04 = new Literal("at");
		at04.addTerm(new NumberTermImpl(0));
		at04.addTerm(new NumberTermImpl(4));

		Capability move_to01 = new Capability(new GBelief(at00), new Action("move_to01"), new GBelief(at01));
		Capability move_to02 = new Capability(new GBelief(at01), new Action("move_to02"), new GBelief(at02));
		Capability move_to03 = new Capability(new GBelief(at02), new Action("move_to03"), new GBelief(at03));
		Capability move_to04 = new Capability(new GBelief(at03), new Action("move_to04"), new GBelief(at04));

		// Add capabilities to environment
		capLibrary.add(move_to01);
		capLibrary.add(move_to02);
		capLibrary.add(move_to03);
		capLibrary.add(move_to04);
	}


	public Unifier executeAction(String agName, Action act) throws AILexception {
		AJPFLogger.info(logname, "Action execution attempted for " + act.toPredicate().toString());
		Unifier theta = new Unifier();
		byte action_state;
		//AJPFLogger.info(logname, act.getFunctor());
		if (act.getFunctor().equals("move_to")) {

			double x = ((NumberTerm) act.getTerm(0)).solve();
			double y = ((NumberTerm) act.getTerm(1)).solve();

			//random chance of applying the move action's post-conds to global percepts.
			if (r.nextBoolean()) {
				Predicate at = new Predicate("at");
				at.addTerm(new NumberTermImpl(x));
				at.addTerm(new NumberTermImpl(y));

				Predicate old_pos = new Predicate("at");

				double robot_x;
				double robot_y;
				double new_robot_x;
				double new_robot_y;

				robot_x = robot1_x;
				robot_y = robot1_y;
				robot1_x = x;
				robot1_y = y;
				new_robot_x = x;
				new_robot_y = y;

				old_pos.addTerm(new NumberTermImpl(robot_x));
				old_pos.addTerm(new NumberTermImpl(robot_y));

				removePercept(old_pos);
				addPercept(at);
			}



			if (x == 0 && y == 1){
				DurativeAction move_to01 = new DurativeAction(act, 5, 3);
				action_state = monitorActionState(agName, move_to01);
				if (action_state == DurativeAction.actionSucceeded){ agentmap.get(agName).getIntention().unsuspend();
				} else if (action_state == DurativeAction.actionFailed) { executeAction(agName, act);
				} else if (action_state == DurativeAction.actionAbort) { done(); }
			}

			if (x == 0 && y == 2){
				DurativeAction move_to02 = new DurativeAction(act, 5, 3);
				action_state = monitorActionState(agName, move_to02);
				if (action_state == DurativeAction.actionSucceeded){ agentmap.get(agName).getIntention().unsuspend();
				} else if (action_state == DurativeAction.actionFailed) { executeAction(agName, act);
				} else if (action_state == DurativeAction.actionAbort) { done(); }
			}

			if (x == 0 && y == 3){
				DurativeAction move_to03 = new DurativeAction(act, 5, 3);
				action_state = monitorActionState(agName, move_to03);
				if (action_state == DurativeAction.actionSucceeded){ agentmap.get(agName).getIntention().unsuspend();
				} else if (action_state == DurativeAction.actionFailed) { executeAction(agName, act);
				} else if (action_state == DurativeAction.actionAbort) { done(); }
			}

			if (x == 0 && y == 4){
				DurativeAction move_to04 = new DurativeAction(act, 2, 1);
				action_state = monitorActionState(agName, move_to04);
				if (action_state == DurativeAction.actionSucceeded){ agentmap.get(agName).getIntention().unsuspend();
				} else if (action_state == DurativeAction.actionFailed) { executeAction(agName, act);
				} else if (action_state == DurativeAction.actionAbort) { done(); }
			}
		}

		try {
			theta = super.executeAction(agName, act);
		} catch (AILexception e) {
			throw e;
		}

		return theta;
	}

	*/
/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#setMAS(ail.mas.MAS)
	 *//*

	public void setMAS(MAS m) {
		super.setMAS(m);
		r = new UniformBoolChoice(m.getController());
	}


// fake clock

	public int clock() {
		seconds++;
		return seconds;
	}


// Set termination conditions here

	public byte updateActionState(String agName, DurativeAction act) {
		AILAgent a = agentmap.get(agName);
		// messy way of getting cap names from actions..
		Predicate actionPredicate = new Predicate(act.getFunctor() + act.getTerm(0) + act.getTerm(1));
		ArrayList<Literal> postConditions = capLibrary.getRelevant(actionPredicate, AILAgent.SelectionOrder.LINEAR).next().postConditionsToLiterals();
		//Need to ask Louise about why these two don't line up - for now, the hack is below (making them both sets of literals before comparing)
		Set<Literal> perceptLiterals = perceptsToLiterals();

		//cycle the clock and record time passed
		int timepassed = clock();

		if (act.duration > 0) {
			if (act.duration > timepassed) {
				// if action duration < clock, set state to active
				act.setState(DurativeAction.actionActive);
			} else {
				act.setState(DurativeAction.actionPending);
			}
		}
		if ((act.duration == 0 || act.duration == timepassed) && !perceptLiterals.containsAll(postConditions)) {
				// if action postconditions =/= percepts
			act.setState(DurativeAction.actionFailed);
			failureCount++;
			agentmap.get(agName).getAFLog().put(act.getFunctor(), failureCount);

			if (a.getAFLog().get(act.getFunctor()) > act.getThreshold(act)){
				act.setState(DurativeAction.actionAbort);
			}
		} else if ((act.duration == 0 || act.duration == timepassed) && perceptLiterals.contains(postConditions)) {
			// if action at least one postcondition is part of percepts
			act.setState(DurativeAction.actionSucceededwithFailure);

		} else if ((act.duration == 0 || act.duration == timepassed) && perceptLiterals.containsAll(postConditions)) {
			// if action postconditions == percepts
			act.setState(DurativeAction.actionSucceeded);
		} else if (act.duration < timepassed && act.duration != 0) {
			// if action duration < clock, set state to timedout
			act.setState(DurativeAction.actionTimedout);

		}


		return act.getActionState();

	}

	public byte monitorActionState(String agName, DurativeAction act) {

		byte action_state;
		
		do { action_state = updateActionState(agName, act);
			AJPFLogger.info(logname, act.getFunctor() + act.getTerm(0) + act.getTerm(1) + " is active.");
		} while (action_state == DurativeAction.actionActive);

		if (action_state == DurativeAction.actionTimedout) {
			AJPFLogger.info(logname, act.getFunctor() + " has timed out.");
			seconds = 0;
			return action_state;
		} else if (action_state == DurativeAction.actionFailed) {
			AJPFLogger.info(logname, act.getFunctor() + " has failed.");
			seconds = 0;
			return action_state;
		} else if (action_state == DurativeAction.actionAbort) {
			AJPFLogger.info(logname, act.getFunctor() + " has been aborted due to excessive failure.");
			seconds = 0;
			return action_state;
		} else if (action_state == DurativeAction.actionSucceededwithFailure) {
			AJPFLogger.info(logname, act.getFunctor() + " has succeeded with failure.");
			seconds = 0;
			return action_state;
		} else if (action_state == DurativeAction.actionSucceeded) {
			agentmap.get(agName).getAFLog().put(act.getFunctor(), failureCount);
			AJPFLogger.info(logname, act.getFunctor() + " has succeeded after " + agentmap.get(agName).getAFLog().get(act.getFunctor()).toString() + " attempt(s).");
			failureCount = 1;
			seconds = 0;
			return action_state;
		} else {
			AJPFLogger.info(logname, "Error with action: " + act.getFunctor() + " - Action state pending.");
			return action_state;
		}
	}

	public Set<Literal> perceptsToLiterals() {
		Set<Literal> p = new HashSet<Literal>();
		if (! percepts.isEmpty()) {
			for (Predicate per: percepts) {
				p.add(new Literal((Predicate) per.clone()));
			}
		}
		return p;
	}

}*/
