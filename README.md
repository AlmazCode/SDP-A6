# Assignment 6 — Social Network, Variant 16

A small console project: Observer handles posts and reactions, while Visitor
performs operations on a community tree. Java 17, Maven, and JUnit 5.
No other libraries are used. The PDF report is prepared separately.

## Running the Project

Requirements: JDK 17 or later and Maven.

```bash
mvn test
java -cp target/classes social.Main
```

The application starts **3 producer threads** by default. Pass the thread count
as an argument to change it; the minimum value is 3:

```bash
java -cp target/classes social.Main 4
```

Each producer publishes 100 events. Producers with even indices publish post
events, while producers with odd indices publish reactions. This simulates an
event stream for existing group snapshots, without a database or user interface.

Main results when running with 3 threads:

```text
Producer threads: 3
Feed events: 200
Reactions in java: 100
Notifications: 600
Logged events: 600
Engagement: 27
Content count: 5
Moderation: reviewed=5, flagged=[c1]
Popular content: [p1, i1, v1]
```

The application also prints the tree as JSON and a producer completion message.
600 log entries = 300 original events + 200 moderation reports + 100 trend updates.

## Project Structure

All classes are in `src/main/java/social`; tests are in `src/test/java/social`.
Classes are small, and each is responsible for one part of the assignment.

| Component | Classes |
| --- | --- |
| Observer interfaces | `Subject<E>`, `Observer<E>` |
| Events and delivery | `SocialEvent`, `EventType`, `EventSource` |
| Two data sources | `PostSource`, `ReactionSource` |
| Five listeners | `Feed`, `NotificationCenter`, `ModerationService`, `TrendingTracker`, `EventLog` |
| Two container types | `Community`, `Group` |
| Four leaf types | `TextPost`, `Comment`, `Image`, `Video` |
| Visitor interfaces | `Element`, `Visitor<R>` |
| Operations | `EngagementVisitor`, `ModerationVisitor`, `PopularContentVisitor`, `JsonVisitor`, `ContentCountVisitor` |
| Composition and threading | `Main`, `Producers` |

## Observer

`PostSource` publishes `POST_CREATED`, and `ReactionSource` publishes
`REACTION_ADDED`. Subscriptions are registered for a specific `EventType`.
A listener can subscribe to multiple event types and sources. Subscribing the
same object to the same event type again does not create a duplicate.

The project uses **push** delivery: the source passes an immutable `SocialEvent`
directly to `onEvent`. Each event contains its type, a snapshot of the related
group, a detail string, and a numeric value. Listeners do not need to query source
state that another thread may already have changed. `SocialEvent` and tree nodes
are records; child lists are copied using `List.copyOf`.

Delivery is synchronous: handlers run in the producer's thread. This simplifies
shutdown: when `publish` returns successfully, both the original event and any
reports it triggered have been processed. A slow handler delays its producer;
this is a deliberate trade-off for a small educational project.

**Observer chain:** `ReactionSource → TrendingTracker → NotificationCenter / EventLog`.
`TrendingTracker` is both an Observer and a Subject. It counts reactions per group
and publishes `TREND_UPDATED` with the accumulated reaction count.

The registry is an `EnumMap` populated in the constructor with
`CopyOnWriteArrayList` values. The map itself does not change after construction.
Each list traversal uses a snapshot:

- Listeners can subscribe and unsubscribe both outside and inside `onEvent`.
- Subscription changes do not affect a traversal that has already started;
  a new snapshot will reflect the change.
- If concurrent publications have already obtained snapshots, an unsubscribed
  listener may still receive those events. Unsubscribing does not cancel handlers
  that have already started.
- One handler's exception does not prevent delivery to the remaining listeners.
  After traversal, `publish` throws an exception; additional failures are attached
  as suppressed exceptions.

## Composite and Visitor

The tree has three levels:

```text
Community: students
├── Group: java
│   ├── TextPost: p1, likes=10
│   ├── Comment: c1, likes=2, text contains SPAM
│   ├── Image: i1, likes=5
│   └── Video: v1, likes=7, durationSeconds=30
└── Group: music
    └── TextPost: p2, likes=3
```

Elements store data and implement `accept`. Calculations, filtering, moderation,
and serialization are implemented by visitors.

| Visitor | Result |
| --- | --- |
| `EngagementVisitor` | `Long`: the sum of likes across all leaves |
| `ModerationVisitor` | `Void`; accumulates the number of reviewed elements and IDs whose text or caption contains `spam`, ignoring case |
| `PopularContentVisitor` | `List<Element>`: content with likes greater than or equal to the supplied threshold |
| `JsonVisitor` | `String`: JSON with nested children and escaped strings |
| `ContentCountVisitor` | `Integer`: the number of leaves; an additional operation demonstrating the trade-off |

**The Visitor owns traversal.** The `accept` method delegates to the concrete
`visit(this)` overload. Each container visitor calls `accept` on its children.
This allows operations to start at any group and preserves correct JSON nesting.
Calling `visitor.visit(element)` on a variable declared as `Element` does not
replace this mechanism: overload resolution uses the argument's declared type.
Inside `accept`, `this` has the concrete node type, providing the second part of
double dispatch.

Hand calculation for the demo: engagement = **10 + 2 + 5 + 7 + 3 = 27**;
there are **5** leaves; at a threshold of 5, popular content is `p1`, `i1`, and
`v1`; moderation reviews 5 elements and flags only `c1`.

## Integration and Concurrency

`POST_CREATED → ModerationService → event.group().accept(ModerationVisitor)
→ MODERATION_COMPLETED → NotificationCenter / EventLog`.

Moderation traverses **only the group from the event**. Other groups in the
community are not inspected. The report returns to the system as a new event:
`value` contains the number of flagged elements, and `detail` contains the number
reviewed and their flagged IDs. Each event gets a separate stateful Visitor, so
its mutable state is not shared between threads.

`Main` is the single composition root: it creates and wires services.
Dependencies are passed through constructors; there are no static services or
lazily created dependencies. `Producers` receives an existing `ExecutorService`.

Shared counters use `AtomicInteger` / `AtomicLong`; the feed and log use
`ConcurrentLinkedQueue`. The trend counter map is created in advance for the
supplied groups and is never structurally modified; group IDs must be unique.
The tree is immutable. No lock is held on the whole Subject.

`Producers.run` submits jobs, waits for their results through `Future.get`, then
calls `shutdown` and waits for all accepted jobs to finish. If one job fails,
the others still finish. Interruption during termination waiting does not cancel
accepted work; the interrupt flag is restored after waiting. `shutdownNow` is
not used, and nested events are handled synchronously, so normal shutdown loses
no events and leaves no worker threads running.

There is no total event order across producers. For example, a trend update with
value 2 may reach the log before value 1; the final counter is still exact.
Engagement uses the likes stored in the tree snapshot, while `TrendingTracker`
counts incoming reaction events. These are different metrics: the simulation
does not modify the tree snapshot.

## Tests

```bash
mvn test
```

**30 JUnit 5 tests:**

| File | Count | Coverage |
| --- | ---: | --- |
| `ObserverTest` | 9 | Event types, duplicate subscriptions, subscription changes inside handlers, listener exceptions, two sources, and the trend chain |
| `VisitorTest` | 9 | Hand calculations, accumulated state, filtering, empty trees, JSON, escaping, and defensive copying of child lists |
| `IntegrationTest` | 5 | Related subtree selection, moderation reports, stress, graceful shutdown, and shutdown after producer failure |
| `ContentCountVisitorTest` | 2 | The new operation and an empty tree |
| `VideoTest` | 5 | Support for the new element in all five visitors |

The stress test runs **4 threads × 1,000 events**. It expects exactly 4,000 original
notifications, 4,000 aggregated events, a final counter of 4,000, and 4,000 distinct
aggregate values. It also checks that the ExecutorService has terminated.

## UML

[Class diagram SVG](docs/uml.svg) · [PNG for the report](docs/uml.png) ·
[Editable Graphviz source](docs/uml.dot).

The diagram shows Observer, Composite, Visitor, and their integration through
`ModerationService`. Graphviz is needed only to regenerate the diagram, not to
run the application:

```bash
dot -Tsvg docs/uml.dot -o docs/uml.svg
dot -Tpng -Gdpi=160 docs/uml.dot -o docs/uml.png
```

## Trade-off: Two Separate Commits

History is saved in `history.bundle`: the project's `.git` directory was read-only
in the environment where it was created. The bundle contains a complete Git
repository with all commits. To restore a normal working copy with its history,
run these commands from the project directory:

```bash
git clone history.bundle social-network
cd social-network
git log --oneline
```

Then replace the local origin with your GitHub repository URL and push the `main`
branch. Submit the link to that GitHub repository.

**`066c3d4` — a new Visitor without changing elements.**

Only these files changed:

- `src/main/java/social/ContentCountVisitor.java` — the new operation;
- `src/test/java/social/ContentCountVisitorTest.java` — its tests;
- `README.md` — documentation.

No element class or Visitor interface changed. Adding operations is inexpensive.

**`08a1dac` — a new element type, Video.**

Files changed:

- `src/main/java/social/Video.java`;
- `src/main/java/social/Visitor.java`;
- `src/main/java/social/EngagementVisitor.java`;
- `src/main/java/social/ModerationVisitor.java`;
- `src/main/java/social/PopularContentVisitor.java`;
- `src/main/java/social/JsonVisitor.java`;
- `src/main/java/social/ContentCountVisitor.java`;
- `src/main/java/social/Main.java`;
- `src/test/java/social/VideoTest.java`;
- `README.md`.

A new element requires an overload in the interface and an implementation in
**every** visitor. If content types change more often than operations, Visitor
becomes expensive to maintain.

After restoring the repository, inspect the changes with:

```bash
git show --stat 066c3d4
git show --stat 08a1dac
```

The commits reflect actual development steps and have real timestamps. The
assignment's requirement for two weeks of development history cannot be
reproduced in a single session; commit dates have not been altered. The PDF report
and live defense are completed separately.
