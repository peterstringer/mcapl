// ----------------------------------------------------------------------------
// Copyright (C) 2013 Louise A. Dennis, and Michael Fisher 
// 
// This file is part of the Engineering Autonomous Space Software (EASS) Library.
// 
// The EASS Library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
// 
// The EASS Library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with the EASS Library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// 
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------

package eass.simple;

import ail.mas.ActionScheduler;
import eass.mas.DefaultEASSEnvironment;
import ail.util.AILexception;
import ail.syntax.Message;
import ail.syntax.Unifier;
import ail.syntax.Action;
import ail.syntax.Literal;
import ail.syntax.Predicate;
import ail.syntax.NumberTermImpl;
import ail.syntax.PredicatewAnnotation;
import ail.semantics.AILAgent;

import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.annotation.FilterField;
//import gov.nasa.jpf.jvm.abstraction.filter.FilterField;
import gov.nasa.jpf.jvm.Verify;

/**
 * Environment for a Trash Robot Scenario;
 * 
 * @author louiseadennis
 *
 */
public class SimpleEnv extends DefaultEASSEnvironment {
	Random r = new Random();
	
	/**
	 * Two performatives, perform and tell.
	 */
	public SimpleEnv() {
		super();
	}
	
		
	
	/**
	 * When a pickup action is executed the environment stores new perceptions
	 * for the agent - that its picked something up and its hands are now longer
	 * empty.
	 */
   public Unifier executeAction(String agName, Action act) throws AILexception {
	   	Unifier theta = new Unifier();

	   	if (act.getFunctor().equals("pickup")) {
	   		Predicate object = (Predicate) act.getTerm(0);
     		removePercept(object);
	   	} else if (act.getFunctor().equals("putdown")) {
	   		Predicate object = (Predicate) act.getTerm(0);
	   		addPercept(object);
        } else if (act.getFunctor().equals("pickup1")) {
        	if (r.nextBoolean()) {
        		addPercept(new Predicate("block1"));
        	}
           	if (r.nextBoolean()) {
        		addPercept(new Predicate("block2"));
        	}
        } else if (act.getFunctor().equals("pickup2")) {
        	if (r.nextBoolean()) {
        		addPercept(new Predicate("block1"));
        	}
        	if (r.nextBoolean()) {
        		addPercept(new Predicate("block2"));
        	}
        } else if (act.getFunctor().equals("random")) {
        	if (r.nextBoolean()) {
        		addPercept(new Predicate("block1"));
        	}
        	if (r.nextBoolean()) {
        		addPercept(new Predicate("block2"));
        	}
       }
	   	
	   	try {
	   		theta = super.executeAction(agName, act);
    	} catch (AILexception e) {
    		throw e;
    	}

    	return theta;
   }
   
	public boolean mThreads() {
		return false;
	}
	/*
	 * (non-Javadoc)
	 * @see ail.others.DefaultEnvironment#separateThread()
	 */
	public boolean separateThread() {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see eass.mas.DefaultEASSEnvironment#done()
	 */
	public boolean done() {
		setDone(true);
		return super.done();
	}

 
}
