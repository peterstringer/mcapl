// ----------------------------------------------------------------------------
// Copyright (C) 2012 Louise A. Dennis,  and Michael Fisher
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
// License along with Gwendolen; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// 
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------

package gwendolen.simple;

import org.junit.Test;

import ail.util.AJPF_w_AIL;

import gov.nasa.jpf.util.test.TestJPF;

/**
 * Tests of various types of property using the simple example Gwendolen programs.
 * Split over several files since JPF was having trouble executing multiple tests in one file.
 * @author louiseadennis
 *
 */
public class Simple10Tests extends TestJPF {

static final String[] PICKUP_ARGS = {
};

  //--- driver to execute single test methods
  public static void main(String[] args) {
	  runTestsOfThisClass(args);
  }
  

  //--- test methods
	  @Test //----------------------------------------------------------------------
	  public void testPropertyInvolvingDroppingGoals () {
		  if (verifyNoPropertyViolation(PICKUP_ARGS)) {
		    	String filename =  "/src/examples/gwendolen/simple/DroppingGoals/DroppingGoals.ail";
		    	String prop_filename =  "/src/examples/gwendolen/simple/DroppingGoals/DroppingGoals.psl";
		    	String[] args = new String[3];
		    	args[0] = filename;
		    	args[1] = prop_filename;
		    	args[2] = "1";
		    	AJPF_w_AIL.run(args);
		  } else {
			    	 
		  }
	  }

	  @Test //----------------------------------------------------------------------
	  public void testIntendToDo () {
		  if (verifyNoPropertyViolation(PICKUP_ARGS)) {
		    	String filename =  "/src/examples/gwendolen/simple/DroppingGoals/DroppingGoals.ail";
		    	String prop_filename =  "/src/examples/gwendolen/simple/DroppingGoals/DroppingGoals.psl";
		    	String[] args = new String[3];
		    	args[0] = filename;
		    	args[1] = prop_filename;
		    	args[2] = "2";
		    	AJPF_w_AIL.run(args);
		  } else {
			    	 
		  }
	  }

	  @Test //----------------------------------------------------------------------
	  public void testIntendToDoSend () {
		  if (verifyNoPropertyViolation(PICKUP_ARGS)) {
		    	String filename =  "/src/examples/gwendolen/rescue/searcher.ail";
		    	String prop_filename =  "/src/examples/gwendolen/rescue/rescue.psl";
		    	String[] args = new String[3];
		    	args[0] = filename;
		    	args[1] = prop_filename;
		    	args[2] = "8a";
		    	AJPF_w_AIL.run(args);
		  } else {
			    	 
		  }
	  }

}
