(ns proletariat.fsm
  "This implementation of an FSM was born out of a desire for a clear way of
  expressing control flow (including defined error handling to allow for
  'fail-operational' states) that was missing in other implementations/design
  patterns.  First, an overview of some of the other implementations considered
  and why they weren't the right fit:

  #### Clojure FSM Library

  The most notable FSM implementation in Clojure is
  [automat](https://github.com/ztellman/automat), which is a relatively strict
  implementation of a [Moore
  Machine](https://en.wikipedia.org/wiki/Moore_machine) with the primary
  intended purpose of pattern matching/validation.  The additional functionality
  it provides are reducers for carrying state information between steps to aid
  in business logic.

  The reasons automat wasn't a fit:

  - Defining a Moore Machine can be more complex and harder to visualize than a
  [Mealy Machine](https://en.wikipedia.org/wiki/Mealy_machine).
  - Wanted the option to manage control flow instead of just pattern matching;
  essentially an [event-driven
  FSM](https://en.wikipedia.org/wiki/Event-driven_finite-state_machine), which
  could receive dynamic events for state transformations instead of receiving an
  initial alphabet of inputs.

  #### Actors / Agents

  Actor implementations have a lot in common with FSM's, but are more geared
  towards event processing.  To draw the parallels: each actor can represent a
  state and the message the actor passes can represent the state transition.
  The differences are related to who and how the events/inputs are generated: in
  an FSM these come from the outside, whereas actors determine where to send
  their messages.  This fits a bit closer to our problem domain, but there were
  still some differences that didn't make it a good fit:

  - Actors (or the Agents implementation in Clojure) are asynchronous.  Our use
  case require synchronous processing of the events to ensure that the data is
  processed fully, end-to-end, in a FIFO manner to ensure data correctness.
  - There is no single control-flow graph or state diagram to provide a clear
  picture of what the processing logic is.  This could be expressed with
  documentation, but having something embedded in the code provides both
  documentation and validation for future maintainers of the code.

  #### Workflow

  The type of processing could be represented well by a workflow engine, such as
  Onyx.  Each step in the workflow performs processing and the next step is
  determined by the result of the processing.  There were two primary
  disadvantages to this approach:

  - Most frameworks were too heavy-weight for what we want to accomplish.
  - Most implementations only allow for a DAG and we needed a way to express
  'cycles' in the sense of a 'fail-operational' state: for example, if there is
  an exception due to a system problem, we will want to sit in the exeption
  state until the system recovers, at which point, restart processing from where
  we left off.

  ---

  ### Requirements

  To address the right level of abstraction, the below requirements were pulled
  from the above:

  - Express control flow in a directed graph (closer to a Mealy Machine style
  FSM)
  - Events as inputs for state transitions
  - Synchronous
  - Lightweight

  The primary goal was for ease of expression/understanding of a complex
  processing flow.

  ---

  ### Implementation

  An FSM fits perfectly on top of a directed graph and Clojure already has an
  excellent graph library in [Loom](https://github.com/aysylu/loom). This made
  the definition of the FSM and the state transition fairly trivial.

  At its base, `proletariat.fsm` implements a Mealy Machine style FSM and can
  perform any standard pattern matching/validation tasks by simply defining the
  states and transitions with `build`.  To map the FSM definition (that an FSM
  is defined by a 5-tuple `(Σ, Q, q0, F, δ)`) to the implementation:

  - Σ *is the set of symbols representing input to the FSM*: this is the
  set of transitions that are the second of the two-tuple inputs to `build`
  - Q *is the set of states of the FSM*: this is the unique set of states (start
  and end) expressed in the first of the two-tuple inputs to `build`
  - q0 ∈ Q *is the start state of the FSM*: the value passed to `start`
  - F ⊆ Q *is the set of final states of the FSM*: this would be the set
  of states that have no outbound edges, which is calculated implicitly in
  `transition`, but could easily be added as a function to obtain the set.
  - δ : Q × Σ → Q *is the transition function*: this is the
  function `transition`.

  This base was extended to add the additional functionality to fulfill our
  requirements with two additions:

  - The `entry-action` and `exit-action` multimethods (taken from a [UML State
  Machine](https://en.wikipedia.org/wiki/UML_state_machine))
  - The `auto-event` multimethod for providing a code based event input
  - The `run-auto` function to execute an automated FSM

  This essentially provided the 'event' style FSM on top of the more traditional
  implementation.
"
  (:require [loom
             [graph :as g]
             [label :as l]
             [io :as gio]]))

(defn view
  "Converts `fsm` to a temporary PNG file using GraphViz and opens it in the
  current desktop environment's default viewer for PNG files.  Requires
  GraphViz's 'dot' (or a specified algorithm) to be installed in
  the shell's path."
  [fsm]
  (gio/view fsm))

(defn build
  "Builds a new Finite State Machine (FSM).  Input should be 2-tuple / input
  expr pairs representing the directed state transitions.  The 2-tuple should
  have the name of the source state and target state making up a transition and
  the expr should have the name of the input that causes the transition.  For
  example:

    (fsm [:a :b] :first
         [:b :c] :second)

  This will create a new FSM that has 3 states: :a, :b, and :c with 2 state
  transitions (:a -> :b and :b -> :c).  The inputs that cause the transitions
  are: :first (for :a -> :b) and :second (for :b -> :c).

  An FSM can be any directed graph and can include cycles.  There is no
  predefined entry point; the application can start wherever it needs.  Exit
  points are any node that does not have any outgoing transitions (in the
  above example :c is an exit node and will generate :accepted? = true).

  You can visualize your FSM using the `view` function.
  "
  [& trans+input]
  (apply l/add-labeled-edges
         (conj
           trans+input
           (apply g/digraph
                  (into #{} (flatten (filter vector? trans+input)))))))

(defrecord FsmOutput [accepted? start-state current-state previous-state])

(defn- next-state
  [fsm current-state input]
  (let [out-edges (g/out-edges fsm current-state)]
    (try
      (as-> out-edges $
            (map (partial apply l/label fsm) $)
            (.indexOf $ input)
            (nth out-edges $)
            (second $))
      (catch IndexOutOfBoundsException _))))

(defn transition
  "Performs a state transition on the provided fsm in the current given state
  for the provided input.  The state can either be a starting state, which will
  simply return the starting FsmOutput, or a previous FsmOutput.  If the input
  provided is not a valid state transition for the fsm an Exception will be
  thrown.  Alternatively, a user can provide an optional parameter for error
  cases to be returned instead of the exception being thrown.  The result of
  this function is an FsmOutput representing the current state of the system.
  "
  ([fsm state input]
   (transition fsm state input nil))
  ([fsm state input err]
   (let [start? (not (instance? FsmOutput state))
         current-state (if start? state (:current-state state))
         new-state (if start? state (next-state fsm current-state input))]
     (if new-state
       (->FsmOutput
         (zero? (g/out-degree fsm new-state))
         (if start? state (:start-state state))
         new-state
         (if start? nil current-state))
       (or err
           (throw
             (IllegalArgumentException.
               (format (str "Current state `%s` has no valid "
                            "transition for input `%s`")
                       current-state
                       input))))))))

(defn start
  "Convenience for starting an FSM from an initial state.  Equivalent to
  calling `(transition fsm state nil)`
  "
  [fsm state]
  (transition fsm state nil))

(defmulti entry-action
  "When invoking an FSM with `run-auto`, the system will look for a multimethod
  implementation to invoke at the entry point of a new state prior to a
  transition.  The entry-action implementation takes the current FsmOutput
  and the response, if not nil, will be added as `:value` to the FsmOutput
  prior to any further processing."
  :current-state)

(defmulti exit-action
  "When invoking an FSM with `run-auto`, the system will look for a multimethod
  implementation to invoke at the exit point of a state prior to the
  transition.  The exit-action implementation takes the interim FsmOutput
  and the response, if not nil, will be added as `:value` to the FsmOutput
  prior to any further processing."
  :current-state)

(defmulti auto-event
  "When invoking an FSM with `run-auto`, the system will look for a multimethod
  implementation to invoke that represents a new event that generates input
  for a state transition.  The `auto-event` implementation will be called with
  the entry FsmOutput (after `entry-action` is run) and the response should be
  either a single value representing the input for the state transition or a
  2-tuple with the first element as the input and the second as a value to be
  added as `:value` to the FsmOutput.  If no `auto-event` is defined, the
  processing will stop as there is no direction for how to proceed."
  :current-state)

(defn- action*
  [f output]
  (if f
    (let [res (f output)]
      (if res (assoc output :value res) output))
    output))

(defn- entry-action*
  [output]
  (action* (get-method entry-action (:current-state output)) output))

(defn- exit-action*
  [output]
  (action* (get-method exit-action (:current-state output)) output))

(def ^:private vectorize
  (comp vec flatten vector))

(defn run
  "Runs an FSM from the provided start-state until completion, or indefinitely
  if no auto-event is defined or if it is not accessed via state transitions.
  The processing will follow this pattern:

    start-state
         |                     ---------------
         v                     |  accepted?  |
    entry-action -> interim -> |   or no     | -no-> auto-event -> exit-action
         ^                     | auto-event? |     (provides input)     |
         |                     ---------------                          v
         |                            |                               output
         |                           yes                                |
         |                            |                                 |
         |                            v                                 |
         |                   return interim state                       |
         |                                                              |
         ----------------------------------------------------------------

  The final FsmOutput will be returned upon termination.

  Optionally, can take a start-value that will be added as the FsmOutput :value
  upon initialization and will be available to the starting state entry-action
  function, if defined.
  "
  ([fsm start-state]
    (run fsm start-state nil))
  ([fsm start-state start-value]
   (loop [o (assoc (start fsm start-state) :value start-value)]
     (let [output        (entry-action* o)
           auto-event-fn (get-method auto-event (:current-state output))]
       (if (and (not (:accepted? output)) auto-event-fn)
         (let [res (vectorize (auto-event-fn output))
               in  (first res)
               v   (or (second res) (:value output))]
           (recur (exit-action* (assoc (transition fsm output in) :value v))))
         output)))))