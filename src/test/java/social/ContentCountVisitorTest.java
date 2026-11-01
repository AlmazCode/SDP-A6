package social;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentCountVisitorTest {
    @Test
    void newOperationCountsLeavesAcrossGroups() {
        Community tree = new Community("students", List.of(
                new Group("java", List.of(new TextPost("p1", "Hi", 10),
                        new Comment("c1", "Reply", 2), new Image("i1", "Photo", 5))),
                new Group("music", List.of(new TextPost("p2", "Song", 3)))));
        assertEquals(4, tree.accept(new ContentCountVisitor()));
    }

    @Test
    void emptyTreeHasNoContent() {
        assertEquals(0, new Community("empty", List.of()).accept(new ContentCountVisitor()));
    }
}
