# TODO

A living list of ideas and design questions for sim4da. Two rules:

1. **Anything ambiguous lives here**, not in code comments or commit
   messages. The repo's source is for decisions that have been made;
   this file is for decisions still in motion.
2. **Open questions are framed as questions.** Once a question gets a
   verdict, the verdict becomes a one-liner and the work gets done — at
   which point the entry leaves this file.

---

## Designing next

### Topology / CompleteTopology

Constrain `send` to declared neighbors. Currently the network is an
implicit complete graph; making the topology a first-class concept
unlocks rings, trees, meshes, partitioned graphs, and adversarial
network layouts.

**Open design questions:**

- *API shape* — declare the topology once before `simulate()`, or allow
  it to evolve during the simulation (link failure, partition healing)?
  Factory style (`Topology.ring(nodes)`, `Topology.tree(...)`,
  `Topology.mesh(...)`), or a graph-builder DSL?
- *Where it lives* — per-node neighbor list on `NodeProxy` (matches the
  "per-node infrastructure handle" framing), or a central adjacency
  table in `Network`? Both?
- *send to non-neighbor* — silent drop (matches the no-exceptions
  principle), or a `sendChecked`-style throwing variant
  (`NotANeighborException`)?
- *broadcast under a topology* — does it respect topology
  (= "send to my neighbors") or stay all-to-all? If both are useful,
  how do we name them?
- `CompleteTopology` is the implicit default, confirmed by the absence
  of any topology declaration.

### BellTower — logical clocks

A per-node logical-clock layer: Lamport timestamps and/or vector
clocks. The "bell tower" rings each event with logical time. State
naturally lives on `NodeProxy` because each node has its own clock.

**Open design questions:**

- *Which first* — Lamport (one integer per node) or vector clocks
  (one integer per node, per node)? Both side-by-side as alternative
  `BellTower` flavors?
- *Stamping policy* — automatic (every `send` increments, every
  `receive` merges), or opt-in per-message? Automatic gives clean
  semantics but couples the clock to every algorithm; opt-in keeps
  algorithms that don't care from paying.
- *Exposure to user code* — `ReceivedMessage.timestamp()` accessor,
  a wrapper record (`Stamped<T extends Message>`), or metadata on
  `MessageInTransit` that the framework logs but the algorithm has
  to read explicitly?
- *Independence* — confirm BellTower works regardless of the chosen
  topology.

---

## Mechanical cleanup (when convenient)

- **`stash/` deletion or revival.** The files in `stash/` reference an
  old key/value Message API; they don't compile against the current
  framework. Either delete outright, or restore that API as a
  `record MapMessage(Map<String, String> entries) implements Message`
  for ad-hoc demos where students don't want to define classes.

---

## Ideas / nice-to-haves

*(Seed — fills as ideas appear.)*
