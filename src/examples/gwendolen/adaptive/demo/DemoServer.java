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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ail.mas.AIL;
import ail.mas.MAS;
import ail.semantics.AILAgent;
import ail.semantics.adaptive.AdaptiveListener;
import ail.syntax.Action;
import ail.syntax.ActionLog;
import ail.syntax.ActionLogEntry;
import ail.syntax.Capability;
import ail.syntax.Deed;
import ail.syntax.DurativeAction;
import ail.syntax.Intention;
import ail.syntax.Literal;
import ail.syntax.Plan;
import ail.syntax.Predicate;
import ail.syntax.ast.GroundPredSets;
import ail.util.AILConfig;
import ajpf.MCAPLcontroller;

/**
 * A live, visual demonstration of adaptive Gwendolen agents.  Runs the thesis scenarios on the
 * real interpreter and streams what the agent and environment do to a web page over a WebSocket.
 * Faults can be injected into the environment while the agent runs.
 *
 * <pre>
 * java gwendolen.adaptive.demo.DemoServer [port]
 * </pre>
 * then open http://localhost:8090.
 *
 * @author Peter Stringer
 */
public class DemoServer {
	static final String DIR = "/src/examples/gwendolen/adaptive/demo/";

	private static DemoServer instance;

	private final WebSocketServer ws;
	private Thread runner;
	private MCAPLcontroller control;
	private MAS mas;
	private JSONObject lastInit;

	/** Milliseconds per waypoint move and per hallway clock tick. */
	volatile int stepMs = 700;
	volatile int tickMs = 350;

	DemoServer(int port) throws Exception {
		Path page = Paths.get(MCAPLcontroller.getFilename(DIR + "index.html"));
		ws = new WebSocketServer(port, page, this::command, this::connected);
	}

	static DemoServer get() {
		return instance;
	}

	public static void main(String[] args) throws Exception {
		int port = args.length > 0 ? Integer.parseInt(args[0]) : 8090;
		instance = new DemoServer(port);
		instance.ws.start();
		System.out.println("Adaptive Gwendolen demo: open http://localhost:" + port + "  (Ctrl-C to quit)");
		Thread.currentThread().join();
	}

	// ----- Messages to the page

	@SuppressWarnings("unchecked")
	void publish(String type, Object... kv) {
		JSONObject o = new JSONObject();
		o.put("type", type);
		for (int i = 0; i + 1 < kv.length; i += 2) {
			o.put(kv[i], kv[i + 1]);
		}
		ws.broadcast(o.toJSONString());
	}

	private synchronized void connected() {
		if (lastInit != null) {
			ws.broadcast(lastInit.toJSONString());
		}
	}

	// ----- Commands from the page

	private void command(String msg) {
		try {
			JSONObject c = (JSONObject) new JSONParser().parse(msg);
			switch ((String) c.get("cmd")) {
			case "run":
				run(c);
				break;
			case "stop":
				stop();
				publish("stopped");
				break;
			case "speed":
				stepMs = ((Number) c.get("stepMs")).intValue();
				tickMs = ((Number) c.get("tickMs")).intValue();
				break;
			case "fault":
				fault((String) c.get("from"), (String) c.get("to"), (String) c.get("actual"));
				break;
			default:
				publish("error", "message", "Unknown command " + c.get("cmd"));
			}
		} catch (Exception e) {
			publish("error", "message", String.valueOf(e));
		}
	}

	private synchronized void fault(String from, String to, String actual) {
		if (mas == null) {
			return;
		}
		Object env = mas.getEnv();
		if (env instanceof DemoWaypointsEnv) {
			DemoWaypointsEnv w = (DemoWaypointsEnv) env;
			if (actual == null || actual.isEmpty()) w.clearFault(from, to); else w.setFault(from, to, actual);
			publish("faults", "faults", new JSONObject(w.getFaults()));
		} else if (env instanceof DemoHallwayEnv) {
			DemoHallwayEnv h = (DemoHallwayEnv) env;
			if (actual == null || actual.isEmpty()) h.clearFault(from, to); else h.setFault(from, to, actual);
			publish("faults", "faults", new JSONObject(h.getFaults()));
		}
	}

	// ----- Running scenarios

	private synchronized void stop() {
		if (control != null) {
			control.stop();
		}
		if (runner != null) {
			runner.interrupt();
			try {
				runner.join(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		runner = null;
		control = null;
	}

	private synchronized void run(JSONObject c) throws Exception {
		stop();
		String scenario = (String) c.get("scenario");
		int threshold = c.get("threshold") == null ? 1 : ((Number) c.get("threshold")).intValue();
		JSONObject faults = c.get("faults") == null ? new JSONObject() : (JSONObject) c.get("faults");

		AILConfig config = new AILConfig();
		config.setProperty("suppress_version", "true");
		config.setProperty("mas.builder", "gwendolen.GwendolenMASBuilder");
		config.setProperty("log.warning", "ail.mas.DefaultEnvironment");
		String agent;
		JSONObject map;
		String program;
		if (scenario.equals("patrol")) {
			agent = "rover";
			map = Maps.waypoints();
			program = Programs.patrol(threshold, ! Boolean.FALSE.equals(c.get("loop")));
			config.setProperty("env", DemoWaypointsEnv.class.getName());
			config.setProperty("waypoints.start", "a");
			for (Object k: faults.keySet()) config.setProperty("waypoints.fault." + k, (String) faults.get(k));
		} else if (scenario.equals("inspection")) {
			agent = "inspector";
			map = Maps.hallway();
			program = Programs.inspection(threshold);
			config.setProperty("env", DemoHallwayEnv.class.getName());
			config.setProperty("hallway.start", "room4");
			config.setProperty("hallway.travel", "3");
			for (Object k: faults.keySet()) config.setProperty("hallway.fault." + k, (String) faults.get(k));
		} else {
			throw new IllegalArgumentException("Unknown scenario " + scenario);
		}
		File f = File.createTempFile("adaptive-demo-", ".gwen");
		f.deleteOnExit();
		Files.write(f.toPath(), program.getBytes(StandardCharsets.UTF_8));
		config.setProperty("mas.file", f.getAbsolutePath());

		GroundPredSets.clear();
		MCAPLcontroller ctl = new MCAPLcontroller(config, "");
		MAS m = AIL.AILSetup(config, ctl);
		AILAgent ag = m.getAg(agent);
		ag.getAdaptiveState().addListener(new Publisher());
		control = ctl;
		mas = m;

		JSONObject init = new JSONObject();
		init.put("type", "init");
		init.put("scenario", scenario);
		init.put("agent", agent);
		init.put("threshold", threshold);
		init.put("map", map);
		init.put("program", program);
		init.put("faults", faults);
		init.put("state", snapshot(ag));
		lastInit = init;
		ws.broadcast(init.toJSONString());

		runner = new Thread(() -> {
			try {
				ctl.begin();
				if (! Thread.currentThread().isInterrupted()) {
					publish("finished", "state", snapshot(ag));
				}
			} catch (Throwable t) {
				publish("error", "message", String.valueOf(t));
			} finally {
				m.cleanup();
			}
		}, "demo-agent");
		runner.setDaemon(true);
		runner.start();
	}

	/** Pause the environment between steps.  Returns false if the run has been stopped. */
	boolean pause(int ms) {
		try {
			Thread.sleep(ms);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	// ----- Describing the agent

	@SuppressWarnings("unchecked")
	static JSONObject snapshot(AILAgent a) {
		JSONObject s = new JSONObject();
		ActionLog log = a.getAdaptiveState().getActionLog();
		JSONArray caps = new JSONArray();
		for (Capability c: a.getCL()) {
			JSONObject o = new JSONObject();
			o.put("text", c.toString());
			o.put("action", c.getCap().toString());
			if (c instanceof DurativeAction) {
				DurativeAction d = (DurativeAction) c;
				o.put("learned", d.isLearned());
				o.put("threshold", d.getThreshold());
				if (c.getCap().isGround()) {
					o.put("failures", log.failureCount(c.getCap()));
					o.put("status", log.status(c.getCap(), d.getThreshold()).toString());
				}
			}
			caps.add(o);
		}
		s.put("capabilities", caps);
		JSONArray plans = new JSONArray();
		for (Plan p: a.getPL().getPlans()) {
			JSONObject o = new JSONObject();
			o.put("trigger", p.getTriggerEvent().toString());
			o.put("body", deeds(p.getBody()));
			plans.add(o);
		}
		s.put("plans", plans);
		JSONObject patches = new JSONObject();
		for (Map.Entry<String, List<Predicate>> e: a.getAdaptiveState().getPatches().entrySet()) {
			patches.put(e.getKey(), strings(e.getValue()));
		}
		s.put("patches", patches);
		return s;
	}

	/** Deeds in execution order (plan bodies and intentions are stacks). */
	@SuppressWarnings("unchecked")
	static JSONArray deeds(List<Deed> stack) {
		ArrayList<Deed> ds = new ArrayList<Deed>(stack);
		Collections.reverse(ds);
		JSONArray out = new JSONArray();
		for (Deed d: ds) {
			out.add(d.toString());
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	static JSONArray strings(List<?> xs) {
		JSONArray out = new JSONArray();
		if (xs != null) {
			for (Object x: xs) {
				out.add(x.toString());
			}
		}
		return out;
	}

	/** Forwards the agent's adaptive reasoning to the page. */
	private final class Publisher implements AdaptiveListener {
		@Override
		public void actionStarted(AILAgent a, Action action, DurativeAction d) {
			Intention i = a.getIntention();
			publish("started", "action", action.toString(), "description", d.toString(),
					"intention", i == null ? new JSONArray() : deeds(i.deeds()));
		}

		@Override
		public void actionTerminated(AILAgent a, ActionLogEntry e) {
			ActionLog log = a.getAdaptiveState().getActionLog();
			publish("outcome", "action", e.getAction().toString(), "outcome", e.getOutcome().toString(),
					"added", strings(e.getAdded()), "deleted", strings(e.getDeleted()),
					"failures", log.failureCount(e.getAction()), "beliefs", beliefs(a));
		}

		@Override
		public void actionDeprecated(AILAgent a, Predicate action, int failures) {
			publish("deprecated", "action", action.toString(), "failures", failures);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void descriptionLearned(AILAgent a, DurativeAction learned, Map<String, Integer> scores) {
			JSONObject sc = new JSONObject();
			sc.putAll(scores);
			publish("learned", "action", learned.getCap().toString(), "description", learned.toString(),
					"scores", sc, "state", snapshot(a));
		}

		@Override
		public void plansReconfigured(AILAgent a, Predicate action, List<Predicate> patch, int changed) {
			publish("reconfigured", "action", action.toString(), "patch", patch == null ? null : strings(patch),
					"changed", changed, "state", snapshot(a));
		}

		@Override
		public void intentionRepaired(AILAgent a, Predicate action, List<Predicate> repair) {
			publish("repaired", "action", action.toString(), "repair", repair == null ? null : strings(repair));
		}

		private JSONArray beliefs(AILAgent a) {
			ArrayList<String> bs = new ArrayList<String>();
			for (Literal l: a.getBB().getAll()) {
				bs.add(ActionLogEntry.stripAnnotations(l).toString());
			}
			Collections.sort(bs);
			return strings(bs);
		}
	}
}
