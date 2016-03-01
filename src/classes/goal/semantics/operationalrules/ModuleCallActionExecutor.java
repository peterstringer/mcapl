package goal.semantics.operationalrules;

import goal.semantics.GOALAgent;
import goal.semantics.GOALRC;
import goal.syntax.ConjGoalBase;
import goal.syntax.GOALModule;
import goal.syntax.MentalModel;
import goal.syntax.MentalState;
import goal.syntax.ModuleCallAction;
import ail.semantics.AILAgent;
import ail.semantics.OSRule;
import ail.syntax.Action;
import ail.syntax.Deed;
import ail.syntax.Goal;
import ail.syntax.GoalBase;
import ail.syntax.Plan;
import ail.syntax.Unifier;
import ail.syntax.VarTerm;

public class ModuleCallActionExecutor extends ActionExecutor {
	GOALModule action_module;
	Unifier u;

	@Override
	public boolean checkPreconditions(AILAgent a) {
		try {
			Deed d = a.getIntention().hdD();
		
			boolean isaction = d.getCategory() == Deed.DAction;
			
			if (isaction) {
				Action action = (Action) d.getContent();
				if (action instanceof ModuleCallAction) {
					action_module = ((ModuleCallAction) action).getModule();
					u = a.getIntention().hdU();
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}

		
		return false;
	}

	@Override
	public void apply(AILAgent a) {
		GOALAgent ag = (GOALAgent) a;
		Unifier moduleSubstitution = getModuleSubti(u);
		// action_module.setModuleSubti(moduleSubstitution);
		
		ConjGoalBase newAttentionSet = getNewFocus(ag.getMentalState(), moduleSubstitution, ag.getFocusGoal());
		/* for (Plan p: action_module.getRules().getPlans()) {
			p.apply(moduleSubstitution);
		} */
		
		if (newAttentionSet != null) {
			ag.getMentalState().focus(newAttentionSet);
		}
		
		ag.setFocusGoal(null);
		GOALRC rc = (GOALRC) ag.getReasoningCycle();
		rc.setCurrentModuleExecuteFully(action_module);
		a.getIntention().tlI(a);


	}

	private ConjGoalBase getNewFocus(MentalState mentalstate,
			Unifier subst, Goal goal) {
        switch (action_module.getFocusMethod()) {
        case NEW:
                // Create new empty goal base to construct a new attention set.
                return new ConjGoalBase();
        case SELECT:
                ConjGoalBase newAttentionSet = new ConjGoalBase();
                newAttentionSet.add(goal);
                return newAttentionSet;
        case FILTER:
                // from the goals that validate the context,
                // choose one randomly
               // return getNewFilterGoals(mentalstate, subst);
        default:
                return null;
        }
	}

	private Unifier getModuleSubti(Unifier un) {
		if (action_module.getType() == GOALModule.ModuleType.ANONYMOUS) {
			return un;
		} else {
			Unifier u = new Unifier();
			for (VarTerm v: action_module.getParams()) {
				if (un.containsVarName(v.getFunctor())) {
					u.unifies(v, u.get(v));
				}
			}
			return u;
		}
	}
	
	/**
	 * Used in testing.
	 * @return
	 */
	public GOALModule getModule() {
		return action_module;
	}

	@Override
	public String getName() {
		return "Module Call Action Executor";
	}

}
