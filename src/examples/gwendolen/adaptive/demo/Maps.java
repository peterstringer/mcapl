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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Layouts of the thesis maps for drawing (Stringer, Figures 7.1 and 7.4).
 *
 * @author Peter Stringer
 */
final class Maps {
	private Maps() {}

	/** The 4-node waypoint map, fully connected. */
	static JSONObject waypoints() {
		JSONObject m = new JSONObject();
		JSONArray nodes = new JSONArray();
		nodes.add(node("a", "A", 0, 0));
		nodes.add(node("b", "B", 1, 0));
		nodes.add(node("c", "C", 0, 1));
		nodes.add(node("d", "D", 1, 1));
		m.put("nodes", nodes);
		m.put("edges", edges("a", "b", "a", "c", "a", "d", "b", "c", "b", "d", "c", "d"));
		m.put("kind", "waypoints");
		return m;
	}

	/** Four rooms off a hallway, with the picture location at its end. */
	static JSONObject hallway() {
		JSONObject m = new JSONObject();
		JSONArray nodes = new JSONArray();
		nodes.add(node("room1", "Room 1", 0.3, 0));
		nodes.add(node("room2", "Room 2", 1.3, 0));
		nodes.add(node("room4", "Room 4", 0.3, 2));
		nodes.add(node("room3", "Room 3", 1.3, 2));
		nodes.add(node("hallway", "Hallway", 0.8, 1));
		nodes.add(node("picture_location", "Fire exit", 2.1, 1));
		m.put("nodes", nodes);
		m.put("edges", edges("room1", "room4", "room2", "room3", "room1", "hallway", "room2", "hallway",
				"room3", "hallway", "room4", "hallway", "hallway", "picture_location"));
		m.put("kind", "hallway");
		return m;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject node(String id, String label, double x, double y) {
		JSONObject n = new JSONObject();
		n.put("id", id);
		n.put("label", label);
		n.put("x", x);
		n.put("y", y);
		return n;
	}

	@SuppressWarnings("unchecked")
	private static JSONArray edges(String... ends) {
		JSONArray es = new JSONArray();
		for (int i = 0; i + 1 < ends.length; i += 2) {
			JSONArray e = new JSONArray();
			e.add(ends[i]);
			e.add(ends[i + 1]);
			es.add(e);
		}
		return es;
	}
}
