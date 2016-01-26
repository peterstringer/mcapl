// ----------------------------------------------------------------------------
// Copyright (C) 2016 Louise A. Dennis,  and Michael Fisher
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
package goal.semantics;

import java.util.ArrayList;
import java.util.HashSet;

import goal.parser.GOALLexer;
import goal.parser.GOALParser;
import goal.syntax.ActionRule;
import goal.syntax.ConjGoalBase;
import goal.syntax.MentalState;
import goal.syntax.ast.Abstract_ActionRule;
import goal.syntax.ast.Abstract_MentalState;
import junit.framework.Assert;
import mcaplantlr.runtime.ANTLRStringStream;
import mcaplantlr.runtime.CommonTokenStream;

import org.junit.Test;

import ail.syntax.Guard;
import ail.syntax.Literal;
import ail.syntax.Predicate;
import ail.syntax.Unifier;


public class RuleGuardsTests {
	@Test public void not_perceptguard() {
		GOALParser parser = parser_for("bel(not(visible(Brick, Color)), percept(visible(Brick, Color)))");
		GOALParser parser2 = parser_for("bel(percept(visible(Brick, Color)), not(visible(Brick, Color)))");
		
		try {
			Abstract_MentalState l = parser.mentalstate();
			Guard m  = l.toMCAPL();
			
			Abstract_MentalState l2 = parser2.mentalstate();
			Guard m2 = l2.toMCAPL();
			
			GOALAgent g = new GOALAgent("agent");
			g.getMentalState().addBB(g.getBB());
			g.getMentalState().addPerceptBase(g.getBB("percepts"));
			Predicate visible1 = new Predicate("visible");
			visible1.addTerm(new Predicate("brick1"));
			visible1.addTerm(new Predicate("blue"));
			g.addBel(new Literal(visible1), g.refertoself());
			
			Predicate visible2 = new Predicate("visible");
			visible2.addTerm(new Predicate("brick2"));
			visible2.addTerm(new Predicate("green"));
			HashSet<Predicate> percepts = new HashSet<Predicate>();
			percepts.add(visible2);
			g.getMentalState().addPercepts(percepts);
			
			Assert.assertTrue(g.believesyn(m2, new Unifier()));
			Assert.assertTrue(g.believesyn(m, new Unifier()));
		}  catch (Exception e) {
			System.err.println(e);
			Assert.assertFalse(true);
		}
	}
		
	@Test public void perceptnotguard() {
		GOALParser parser = parser_for(" bel(atBrick(Brick), percept(not(atBrick(Brick))))");
			
		try {
			Abstract_MentalState l = parser.mentalstate();
			Guard m  = l.toMCAPL();
				
				
			GOALAgent g = new GOALAgent("agent");
			g.getMentalState().addBB(g.getBB());
			g.getMentalState().addPerceptBase(g.getBB("percepts"));
			Predicate visible1 = new Predicate("atBrick");
			visible1.addTerm(new Predicate("brick1"));
			g.addBel(new Literal(visible1), g.refertoself());
				
			Predicate visible2 = new Predicate("atBrick");
			visible2.addTerm(new Predicate("brick1"));
				
			Assert.assertTrue(g.believesyn(m, new Unifier()));
		}  catch (Exception e) {
			System.err.println(e);
			Assert.assertFalse(true);
		}

		
	}
	

	
	GOALParser parser_for(String s) {
		GOALLexer lexer = new GOALLexer(new ANTLRStringStream(s));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GOALParser parser = new GOALParser(tokens);
		return parser;
	}

}
