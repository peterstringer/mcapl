/*
package ail.syntax;

import gwendolen.GwendolenMASBuilder;
import gwendolen.failuredetection.DurativeActionEnv;
import org.junit.Test;

import ail.mas.MAS;
import ail.semantics.AILAgent;
import ail.semantics.operationalrules.HandleActionwProblem;
import ail.util.AILPrettyPrinter;
import ajpf.MCAPLcontroller;
import junit.framework.Assert;

public class ActionSuspendsIntentionQuickTests {

	
	*/
/*
	 * Test that the preconditions of the rule are true when required.
	 *//*

	
	@Test
	public void preConditionsMet() {	
		// --- Agent & Intention Setup ---
		
		//Create new AIL agent & other required bits
		AILAgent ag1 = new AILAgent();
		Unifier u = new Unifier();
		Goal g1 = new Goal("goal1", Goal.achieveGoal);
		Action a1 = new Action("action1");
		
		//Create new intention with a goal
		Intention i = new Intention(g1, AILAgent.refertoself(), new AILPrettyPrinter());
		ag1.addGoal(g1);
		//Assign intention to agent
		ag1.setIntention(i);
		//Send a new Intention containing sent action deed to top of stack
		i.iCons(new Event(Event.AILAction), new Deed(a1), new Guard(new GBelief()), u);
		ag1.setIntention(i);
		//Set the agent to complete the new intention
		HandleActionwProblem suspendintention = new HandleActionwProblem();
		// Test that TRUE is returned when checking the required preconditions for the rule
	
		Assert.assertTrue(suspendintention.checkPreconditions(ag1));
	}

	
	*/
/*
	 * Test that the intention is suspended when an action is performed.
	 *//*

	
	@Test public void intentionSuspend() {
    	String filename = "/src/examples/gwendolen/failuredetection/intentionsuspension/ExecuteOneAction.gwen";
		try {
    		String abs_filename = MCAPLcontroller.getFilename(filename);
    		MAS mas = (new GwendolenMASBuilder(abs_filename, true)).getMAS();
    		DurativeActionEnv env = new DurativeActionEnv();

    		mas.setEnv(env);
    		env.setMAS(mas);
    		env.init_after_adding_agents();
    		
    		// Get the active agent (Not EASS abstraction)
    		AILAgent ag1 = env.getAgents().get(0);

    		// Bit of a hack to cycle to action execution rule
    		ag1.reason();
    		ag1.reason();
    		ag1.reason();
    		ag1.reason();


			Assert.assertTrue((ag1.getIntention()).suspended());
    	} catch (Exception e) {
    		System.err.println(e.getMessage());
    		Assert.fail(e.getMessage());
    	}
		
	} 
}
*/
