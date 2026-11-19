# Adaptive Gwendolen agents

Gwendolen agents that notice when their actions stop behaving as described, learn new
descriptions of those actions, and patch their plans to match. This implements the framework
in Peter Stringer's thesis *Adaptable Beliefs-Desires-Intentions Reasoning* (Chapters 4–6),
using plan reconfiguration from Cardoso et al. (2019).

## Running the examples

```
java ail.mas.AIL src/examples/gwendolen/adaptive/patrol.ail
java ail.mas.AIL src/examples/gwendolen/adaptive/inspection.ail
```

* `patrol` is the 4-node waypoint patrol from Section 7.3.1. `move(b,d)` has been changed so
  that it takes the rover to `c`. The rover detects the failure, learns
  `{at(b)} move(b,d) {at(c), ~at(b)}`, patches its route to
  `move(a,b), move(b,d), move(c,d), move(d,c), move(c,b), move(b,a)` (Equation 7.4) and patrols
  again without failing.
* `inspection` is the hallway inspection from Sections 5.3 and 7.2.1. Moves take time. Uncomment
  the fault in `inspection.ail` to leave the robot stuck in the hallway: the move overruns its
  duration, is aborted and the inspection fails.

`log.info = ail.semantics.adaptive` logs the action log, deprecations, learned descriptions and
plan patches.

## Live visual demo

`demo/` runs the scenarios on the interpreter and streams them to a web page over a WebSocket:

```
ant compile
java -cp "bin:lib/3rdparty/*" gwendolen.adaptive.demo.DemoServer   # then open http://localhost:8090
```

Use JDK 11 or later. The page shows the map with the robot moving, the adaptation loop of Figure 4.2,
the action log (Figures 5.1–5.4), the candidate post-conditions and their scores (Figure 6.3),
and the plan and capability libraries as they change. *Break an action* changes what an action does
in the environment while the agent is running, without telling the agent, so you can watch it
detect the change, learn, and patch its plans. The server listens on localhost only, uses only the
JDK (`demo/WebSocketServer.java`), and the agent reports its reasoning through
`ail.semantics.adaptive.AdaptiveListener`.

## Describing actions

Actions are described in a `:Capabilities:` section between `:Initial Beliefs:` (or
`:Reasoning Rules:`) and `:Initial Goals:`:

```
:Capabilities:
{at(a)} move(a,b) {at(b), ~at(a)}
{at(hallway)} move(hallway,exit) {at(exit), ~at(hallway)} [failure {at(entrance)}, abort {blocked}, duration 300, threshold 4]
```

Each capability is `{pre} action {post}`, optionally followed by annotations:

| Annotation          | Meaning                                                           | Default                              |
|---------------------|-------------------------------------------------------------------|--------------------------------------|
| `success {...}`     | the action has succeeded                                          | the post-condition                   |
| `failure {...}`     | the action has failed while still executing                       | none                                 |
| `abort {...}`       | the action must be stopped and not attempted again                | none                                 |
| `duration N`        | the action is aborted if it runs for more than N time units       | 0 (no limit)                         |
| `threshold N`       | failures in the action log before the action is deprecated        | 1                                    |

Conditions are comma-separated literals. `~l` means *l is not believed* in a condition, and *l
stops being believed* in a post-condition. Post-conditions should list what an action deletes
as well as what it adds (`~at(a)` above), because plans are patched by STRIPS planning.
Capabilities may contain variables if they are bound by the pre-condition, e.g.
`{at(X), link(X,Y)} move(X,Y) {at(Y), ~at(X)}`.

Actions without a capability keep the ordinary Gwendolen semantics, and agents without
capabilities behave exactly as before.

## What the agent does

Following Figure 4.3, Stage D of the reasoning cycle is extended:

1. **Execute** (`HandleDurativeAction`, Rules 5.1–5.8). The action is executed and its
   intention is suspended until a terminating condition is believed. Outcomes are judged only
   after the agent has processed the percepts the action caused. Success, then abort (an abort
   condition or a timeout), then failure are checked in that order. An action that the
   environment reports as finished without succeeding has failed. Every outcome is recorded in
   the bounded action log with the change in beliefs it caused.
2. **Detect.** On success the action is removed from the intention. A failure below the
   threshold is retried (Rule 5.5). On an abort the action is stopped in the environment
   (`abort(a)`), the goal's failure event `-!g` is raised and the goal is dropped. When failures
   reach the threshold the action is *deprecated*.
3. **Learn** (Stage D1, `LearnActionDescription`, Algorithm 2). Each failure since the last
   learning step is weighted by recency (1, 2, 3, …). Weights of identical belief changes are
   summed and the highest wins, with ties going to the most recent. The result is a new
   description of that specific action. It takes precedence over any more general description,
   and the failure count is reset.
4. **Reconfigure** (Stage D2, `ReconfigurePlans`, Section 6.3.2). A STRIPS planner looks for a
   way from the action's original pre-condition to its original post-condition using every
   current description. That patch replaces the action throughout the plan library. The
   intention that was executing the action continues with a plan from the agent's current
   beliefs. If there is none, its goal fails.

Environments that keep time implement `ail.mas.TimedEnvironment`. `HallwayEnv` shows how to
report actions that are still executing (`executing`), advance time, and wake agents so that
timeouts are noticed.

## Design decisions beyond the thesis

* **Retries return to the pre-condition first.** In the thesis experiments the rover was returned
  to its starting point before a failed action was retried. That is generalised here: if a
  failed action's pre-condition no longer holds, the agent plans back to it with its own actions
  before retrying. Without this a retry from the wrong place logs a spurious "no change" failure,
  which would then be learned.
* **Abort fails the goal.** Rule 5.7 removes an aborted action. Continuing with the rest of the
  plan would act on an outcome that never happened, so the goal fails instead, using Gwendolen's
  usual `-!g` handling.
* **Patches are computed from the original description,** and plans are always rewritten from
  their original bodies. An action that is deprecated again gets its patch replaced, not patched
  on top. A patch that contains another patched action is expanded.
* **The planner runs inside the agent** (breadth-first, bounded depth and node count) rather than
  as an external Fast Downward process. Patches are therefore shortest, runs are deterministic,
  and agents can still be model checked.
* **Only ground occurrences of an action are patched.** A plan step such as `move(X,Y)` cannot
  take a patch for one instance of it. Such steps rely on the learned description and on the
  repair of the running intention.
