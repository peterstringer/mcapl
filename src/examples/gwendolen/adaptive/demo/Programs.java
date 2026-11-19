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
package gwendolen.adaptive.demo;

/**
 * Gwendolen programs for the demo scenarios, generated so that the failure threshold can be chosen.
 *
 * @author Peter Stringer
 */
final class Programs {
	private Programs() {}

	private static String move(String from, String to, String annotation) {
		return "{at(" + from + ")} move(" + from + "," + to + ") {at(" + to + "), ~at(" + from + ")}" + annotation + "\n";
	}

	private static String annotation(int threshold, int duration) {
		String s = "";
		if (duration > 0) s += "duration " + duration;
		if (threshold != 1) s += (s.isEmpty() ? "" : ", ") + "threshold " + threshold;
		return s.isEmpty() ? "" : " [" + s + "]";
	}

	/** The 4-node patrol of Section 7.3.1 (Equation 7.1), optionally repeated until stopped. */
	static String patrol(int threshold, boolean loop) {
		StringBuilder p = new StringBuilder("GWENDOLEN\n\n:name: rover\n\n:Initial Beliefs:\n\n:Capabilities:\n");
		String[] w = { "a", "b", "c", "d" };
		for (String x: w) {
			for (String y: w) {
				if (! x.equals(y)) {
					p.append(move(x, y, annotation(threshold, 0)));
				}
			}
		}
		p.append("\n:Initial Goals:\npatrol [perform]\n\n:Plans:\n");
		if (loop) {
			p.append("+!patrol [perform] : {True} <- +!route [perform], +!patrol [perform];\n");
		} else {
			p.append("+!patrol [perform] : {True} <- +!route [perform], +!route [perform], print(patrolled);\n");
		}
		p.append("+!route [perform] : {B at(a)} <- move(a,b), move(b,d), move(d,c), move(c,b), move(b,a);\n");
		return p.toString();
	}

	/** The hallway inspection of Sections 5.3 and 7.2.1 (Figures 7.5 and 7.6). */
	static String inspection(int threshold) {
		String a = annotation(threshold, 6);
		StringBuilder p = new StringBuilder("GWENDOLEN\n\n:name: inspector\n\n:Initial Beliefs:\n\n:Capabilities:\n");
		p.append(move("room1", "room4", a)).append(move("room4", "room1", a));
		p.append(move("room2", "room3", a)).append(move("room3", "room2", a));
		for (String r: new String[] { "room1", "room2", "room3", "room4" }) {
			p.append(move(r, "hallway", a)).append(move("hallway", r, a));
		}
		p.append(move("hallway", "picture_location", a)).append(move("picture_location", "hallway", a));
		p.append("{at(picture_location)} take_picture {}\n");
		p.append("\n:Initial Goals:\nroute [perform]\n\n:Plans:\n");
		p.append("+!route [perform] : {B at(room4)} <- move(room4,hallway), move(hallway,picture_location), take_picture,\n"
				+ "    move(picture_location,hallway), move(hallway,room4), print(inspected);\n");
		p.append("-!route [perform] : {True} <- print(inspection_failed);\n");
		return p.toString();
	}
}
