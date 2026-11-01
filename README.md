# Assignment 6 — Social network (variant 16)

Java 17, Maven, JUnit 5. Observer: posts and reactions notify feed,
notifications, moderation, trending and event log. Visitor: immutable
Community → Group → content tree, engagement, moderation, filter and JSON.

```bash
mvn test
java -cp target/classes social.Main 3
```

Push delivery is synchronous. CopyOnWriteArrayList provides subscription
snapshots; handler changes apply to later snapshots. Visitors own traversal.
Each producer finishes its handlers before its job completes, and Producers
shuts down the executor only after accepting jobs and drains them on failure.

## Trade-off: new operation

`ContentCountVisitor` counts content leaves. Only
`src/main/java/social/ContentCountVisitor.java`,
`src/test/java/social/ContentCountVisitorTest.java` and `README.md` changed.
No element class or Visitor interface changed: adding operations is cheap.
