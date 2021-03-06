// ----------------------------------------------------------------------------
// Copyright (C) 2014 Louise A. Dennis, and  Michael Fisher 
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

package actiononly.semantics;

import java.util.Iterator;
import java.util.LinkedList;

import ail.semantics.OSRule;
import ail.semantics.RCStage;

// import gov.nasa.jpf.jvm.abstraction.filter.FilterField;
import gov.nasa.jpf.annotation.FilterField;

/**
 * An Action Reasoning stage
 * Specific reasoning stage.
 * 
 * @author louiseadennis
 *
 */
public class ActionOnlyStage implements RCStage {
	/**
	 * An integer representing the stage.  These are named in the
	 * Gwendolen Reasoning Cycle object.
	 */
	@FilterField
	private int stage;
	/**
	 * The name of the stage.
	 */
	@FilterField
	private String name;
	/**
	 * The disjunction of rules for this stage.
	 */
	@FilterField
	private LinkedList<OSRule> rules = new LinkedList<OSRule>();
		
	/**
	 * Construct a stage from an integer and stage name.
	 * 
	 * @param st an integer representing the stage.
	 * @param s The name of the stage.
	 */
	public ActionOnlyStage(int st, String s) {
		stage = st;
		name = s;
	}
	
	/**
	 * Getter method for the number representing the stage.
	 * 
	 * @return the number representing the stage.
	 */
	private int getStageNum() {
		return stage;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ail.semantics.RCStage#getStageName()
	 */
	public String getStageName() {
		return name;
	}

	/**
	 * Compare two stages
	 * 
	 * @param arg0 a Gwendolen Reasoning Cycle Stage
	 * @return 1 if the stages are the same, 0 if they are not.
	 */
	public int compareTo(ActionOnlyStage arg0) {
		if (arg0.getStageNum() == stage) {
			return 1;
		}
		return 0;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ail.semantics.RCStage#getStageRules()
	 */
	public Iterator<OSRule> getStageRules() {
			return(rules.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see ail.semantics.RCStage#setRule(ail.semantics.OSRule)
	 */
	public void setRule(OSRule rule) {
			rules.add(rule);
	}

}
