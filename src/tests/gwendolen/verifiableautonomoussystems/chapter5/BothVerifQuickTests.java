// ----------------------------------------------------------------------------
// Copyright (C) 2014 Louise A. Dennis,  and Michael Fisher
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

package gwendolen.verifiableautonomoussystems.chapter5;

import org.junit.Test;

import ail.util.AJPF_w_AIL;
import gov.nasa.jpf.util.test.TestJPF;


/**
 * Simple test that an auction example works.
 */
public class BothVerifQuickTests extends TestJPF {

  static final String[] JPF_ARGS = {  "-show" 
  };



  //--- driver to execute single test methods
  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  //--- test methods

 
  @Test //----------------------------------------------------------------------
  public void prop1 () {
    if (verifyNoPropertyViolation(JPF_ARGS)){
    	String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
    	String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
    	String[] args = new String[3];
    	args[0] = filename;
    	args[1] = prop_filename;
    	args[2] = "1";
    	AJPF_w_AIL.run(args);
 	 }
  }
  
  @Test //----------------------------------------------------------------------
  public void prop2 () {
    if (verifyNoPropertyViolation(JPF_ARGS)){
    	String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
    	String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
    	String[] args = new String[3];
    	args[0] = filename;
    	args[1] = prop_filename;
    	args[2] = "2";
    	AJPF_w_AIL.run(args);
 	 }
  }  
  
  @Test //----------------------------------------------------------------------
  public void prop3 () {
	    if (verifyNoPropertyViolation(JPF_ARGS)){
	    	String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
	    	String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
	    	String[] args = new String[3];
	    	args[0] = filename;
	    	args[1] = prop_filename;
	    	args[2] = "3";
	    	AJPF_w_AIL.run(args);
	 	 }
	  }  
  
  @Test //----------------------------------------------------------------------
  public void prop4 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "4";
		  AJPF_w_AIL.run(args);
	  }
  }   
  
  @Test //----------------------------------------------------------------------
  public void prop5 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "5";
		  AJPF_w_AIL.run(args);
	  }
  }   
  
  @Test //----------------------------------------------------------------------
  public void prop6 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "6";
		  AJPF_w_AIL.run(args);
	  }
  }   
  
  @Test //----------------------------------------------------------------------
  public void prop7 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "7";
		  AJPF_w_AIL.run(args);
	  }
  }   
  
  @Test //----------------------------------------------------------------------
  public void prop8 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "8";
		  AJPF_w_AIL.run(args);
	  }
  }   
  
  @Test //----------------------------------------------------------------------
  public void prop9 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "9";
		  AJPF_w_AIL.run(args);
	  }
  } 
  
  
  @Test //----------------------------------------------------------------------
  public void prop10 () {
	  if (verifyNoPropertyViolation(JPF_ARGS)){
		  String filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/car_both_verif.ail";
		  String prop_filename =  "/src/examples/gwendolen/verifiableautonomoussystems/chapter5/cars.psl";
		  String[] args = new String[3];
		  args[0] = filename;
		  args[1] = prop_filename;
		  args[2] = "10";
		  AJPF_w_AIL.run(args);
	  }
  } 



}
