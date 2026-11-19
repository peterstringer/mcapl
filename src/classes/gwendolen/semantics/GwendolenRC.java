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

package gwendolen.semantics;

import java.util.LinkedList;

import ail.semantics.ReasoningCycle;
import ail.semantics.RCStage;
import ail.semantics.AILAgent;
import ail.semantics.operationalrules.*;
import ail.syntax.Action;
import ail.syntax.Goal;

import ajpf.MCAPLcontroller;
// import gov.nasa.jpf.jvm.abstraction.filter.FilterField;
import gov.nasa.jpf.annotation.FilterField;
/**
 * A Gwendolen Reasonning Cycle.  An Example of how to set up a language
 * specific reasoning cycle.  Likely to change as the implementation of
 * OSRule's changes in AIL.
 * 
 * @author louiseadennis
 *
 */
public class GwendolenRC implements ReasoningCycle {
	/**
	 * Place holder for the current stage of the reasoning cycle.
	 */
	private GwendolenRCStage currentstage;

	/**
	 * Set up the reasoning cycle stages.
	 */
	private GwendolenRCStage StageA = new GwendolenRCStage(0, "StageA");
	private GwendolenRCStage StageB = new GwendolenRCStage(1, "StageB");
	private GwendolenRCStage StageC = new GwendolenRCStage(2, "StageC");
	private GwendolenRCStage StageD = new GwendolenRCStage(3, "StageD");
	private GwendolenRCStage StageE = new GwendolenRCStage(4, "StageE");
	private GwendolenRCStage StageF = new GwendolenRCStage(5, "StageF");
	// Stages for adapting to deprecated actions (Stringer, Figure 4.3).  These are only
	// entered when the agent has actions with durative descriptions in its capability library.
	private GwendolenRCStage StageD1 = new GwendolenRCStage(6, "StageD1");
	private GwendolenRCStage StageD2 = new GwendolenRCStage(7, "StageD2");
	private GwendolenRCStage StageE2 = new GwendolenRCStage(8, "StageE2");
	/**
	 * Flag indicating whether this is a point where the properties of the 
	 * multi-agent system should be checked.
	 */
	@FilterField
	private boolean stopandcheck = false;
	private boolean interrupted = false;
			
	/**
	 * Start at Stage A.
	 *
	 */
	public GwendolenRC() {
		currentstage = StageE;
		
		/* Stage A Rules */
		// First we create versions of the rules we want in Stage A
		SelectIntentionNotUnplannedProblemGoal rule1 = new SelectIntentionNotUnplannedProblemGoal();
		SleepIfEmpty rule2 = new SleepIfEmpty();
		DropIntentionIfEmpty rule3 = new DropIntentionIfEmpty();
		RCStage StageA = getStageA();
		StageA.setRule(rule1);  
		StageA.setRule(rule2);
		StageA.setRule(rule3);
		
		/* Stage B Rules */
		
		GenerateApplicablePlansIfNonEmpty rule5 = new GenerateApplicablePlansIfNonEmpty();
		GenerateApplicablePlansEmptyProblemGoal rule6 = new GenerateApplicablePlansEmptyProblemGoal();
		
		RCStage StageB = getStageB();
		StageB.setRule(rule5);
		StageB.setRule(rule6);
		
		/* Stage C Rules */
		
		ApplyApplicablePlans rule7 = new ApplyApplicablePlans();
		
		RCStage StageC = getStageC();
		StageC.setRule(rule7);
		
		/* Stage D Rules */
		
		HandleGuardNotSatisfied rule8 = new HandleGuardNotSatisfied();
		HandleEmptyDeedStack rule9 = new HandleEmptyDeedStack();
		HandleAddAchieveTestGoalwEvent rule10 = new HandleAddAchieveTestGoalwEvent();
		HandleAddPerformGoalwEvent rule10a = new HandleAddPerformGoalwEvent();
		HandleAddContent rulean = new HandleAddContentwEvent();
		HandleAddContext ruleax = new HandleAddContext();

		LinkedList<Integer> excludedGoals = new LinkedList<Integer>();
		excludedGoals.add(new Integer(Goal.maintainGoal));
		HandleDropGeneralGoal rule11 = new HandleDropGeneralGoal(excludedGoals);

		HandleAddBeliefwEvent rule12 = new HandleAddBeliefwEvent();
		HandleDropBeliefwEvent rule13 = new HandleDropBeliefwEvent();
		HandleLockUnLock rule14 = new HandleLockUnLock();
		HandleWaitFor rulehwf = new HandleWaitFor();
		IgnoreUnplannedProblemGoal ruleiup = new IgnoreUnplannedProblemGoal();
		HandleAddPlan rulehap = new HandleAddPlan();
		
		// Set up list of actions with special semantics.
		LinkedList<Integer> excludedActions = new LinkedList<Integer>();
		excludedActions.add(new Integer(Action.sendAction));
		excludedActions.add(new Integer(Action.synchronisedJoin));
		excludedActions.add(new Integer(Action.synchronisedAdopt));
		HandleGeneralAction rule15 = new HandleGeneralAction(excludedActions);
		HandleSendAction rule15a = new HandleSendAction();
		HandleNull rulehn = new HandleNull();
		
		RCStage StageD = getStageD();
		StageD.setRule(rule8);
		StageD.setRule(rule9);
		StageD.setRule(rule10);
		StageD.setRule(rule10a);
		StageD.setRule(rulean);
		StageD.setRule(ruleax);
		StageD.setRule(rule11);
		StageD.setRule(rule12);
		StageD.setRule(rule13);
		StageD.setRule(rule14);
		StageD.setRule(rulehwf);
		StageD.setRule(rulehap);
//		StageD.setRule(rulehac);
		StageD.setRule(new HandleDurativeAction());
		StageD.setRule(rule15);
		StageD.setRule(rule15a);
		StageD.setRule(ruleiup);
		StageD.setRule(rulehn);
		
		/* Stage E Rules */
		
		Perceive rule16 = new Perceive();
		getStageE().setRule(rule16);
		
		/* Stage D1, D2 and E2 Rules */
		StageD1.setRule(new LearnActionDescription());
		StageD2.setRule(new ReconfigurePlans());
		StageE2.setRule(new CheckExecutingActions());
		
		/* Stage F Rules */
		
		HandleMessages rule17 = new HandleMessages();
		getStageF().setRule(rule17);
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.ReasoningCycle#cycle(ail.semantics.AILAgent)
	 */
	public void cycle(AILAgent ag) {
		if (currentstage  == StageA) {
			if (ag.getIntention() != null && ag.getIntention().empty() && (ag.getIntentions().isEmpty() || ag.allintentionssuspended())) {
				setStopandCheck(true);
				currentstage = StageE;
			} else if (ag.getIntention() == null) {
				setStopandCheck(true);
				currentstage = StageE;
			} else {
				currentstage = StageB;
			}
		} else if (currentstage == StageB) {
			currentstage = StageC;
		} else if (currentstage == StageC) {
			setStopandCheck(true);
			if (ag.getIntention() != null & ag.getIntention().empty()) {
				currentstage = StageE;
			} else {
				currentstage = StageD;
			}
		} else if (currentstage == StageD) {
			if (ag.lastruleexecuted.equals("Handle General Action")) {
				setStopandCheck(true);
			}
			if (ag.lastruleexecuted.equals("Handle Send Action")) {
				setStopandCheck(true);
			}
			setStopandCheck(true);
			if (ag.getAdaptiveState().hasPendingDeprecation()) {
				currentstage = StageD1;
			} else {
				currentstage = StageE;
			}
		} else if (currentstage == StageD1) {
			currentstage = StageD2;
		} else if (currentstage == StageD2) {
			currentstage = StageE;
		} else if (currentstage == StageE) {
			if (ag.getAdaptiveState().hasExecutingActions()) {
				currentstage = StageE2;
			} else {
				currentstage = StageF;
			}
		} else if (currentstage == StageE2) {
			currentstage = StageF;
		} else if (currentstage == StageF) {
			currentstage = StageA;
		}

	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.ReasoningCycle#getStage()
	 */
	public RCStage getStage() {
		return currentstage;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.ReasoningCycle#stopandcheck()
	 */
	public boolean stopandcheck() {
		boolean tmp = stopandcheck;
		return tmp;
	} 
	
	public void setStopandCheck(boolean b) {
		stopandcheck = b;
	}
	

	public RCStage getStageA() {
		return StageA;
	}
	public RCStage getStageB() {
		return StageB;
	}
	public RCStage getStageC() {
		return StageC;
	}
	public RCStage getStageD() {
		return StageD;
	}
	public RCStage getStageE() {
		return StageE;
	}
	public RCStage getStageF() {
		return StageF;
	}
	
	public RCStage getStageD1() {
		return StageD1;
	}
	
	public RCStage getStageD2() {
		return StageD2;
	}
	
	public RCStage getStageE2() {
		return StageE2;
	}

	public void setCurrentStage(RCStage rcs) {
		currentstage = (GwendolenRCStage) rcs;
	}
	
	public boolean not_interrupted() {
		return !interrupted;
	}


}
