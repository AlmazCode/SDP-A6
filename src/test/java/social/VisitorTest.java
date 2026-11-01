package social;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VisitorTest {
    private final TextPost post = new TextPost("p1", "Hello", 10);
    private final Comment comment = new Comment("c1", "SPAM link", 2);
    private final Image image = new Image("i1", "Photo", 5);
    private final Group group = new Group("java", List.of(post, comment, image));
    private final Community community = new Community("students", List.of(group,
            new Group("music", List.of(new TextPost("p2", "Music", 3)))));

    @Test
    void engagementMatchesHandCalculatedTotal() {
        // 10 + 2 + 5 + 3 = 20 reactions.
        assertEquals(20L, community.accept(new EngagementVisitor()));
    }

    @Test
    void engagementCanVisitOneSubtree() {
        assertEquals(17L, group.accept(new EngagementVisitor()));
    }

    @Test
    void moderationAccumulatesStateAcrossGroups() {
        ModerationVisitor visitor = new ModerationVisitor();
        community.accept(visitor);
        assertEquals(4, visitor.reviewed());
        assertEquals(List.of("c1"), visitor.flaggedIds());
    }

    @Test
    void filterReturnsActualElementsAndIncludesThreshold() {
        assertEquals(List.of(post, image), community.accept(new PopularContentVisitor(5)));
    }

    @Test
    void filterCanReturnEmptyList() {
        assertEquals(List.of(), community.accept(new PopularContentVisitor(11)));
    }

    @Test
    void jsonKeepsContainerNestingAndLeafData() {
        Community tree = new Community("students", List.of(
                new Group("java", List.of(new TextPost("p1", "Hello", 10)))));
        assertEquals("{\"type\":\"community\",\"id\":\"students\",\"children\":["
                + "{\"type\":\"group\",\"id\":\"java\",\"children\":["
                + "{\"type\":\"post\",\"id\":\"p1\",\"text\":\"Hello\",\"likes\":10}]}]}",
                tree.accept(new JsonVisitor()));
    }

    @Test
    void jsonEscapesQuotesBackslashesAndControlCharacters() {
        TextPost special = new TextPost("p\"1", "a\"b\\c\n\t\r\b\f\u0001", 0);
        assertEquals("{\"type\":\"post\",\"id\":\"p\\\"1\",\"text\":\"a\\\"b\\\\c\\n\\t\\r\\b\\f\\u0001\",\"likes\":0}",
                special.accept(new JsonVisitor()));
    }

    @Test
    void emptyContainersHaveValidVisitorResults() {
        Community empty = new Community("empty", List.of());
        assertEquals(0L, empty.accept(new EngagementVisitor()));
        assertEquals(List.of(), empty.accept(new PopularContentVisitor(0)));
        assertEquals("{\"type\":\"community\",\"id\":\"empty\",\"children\":[]}",
                empty.accept(new JsonVisitor()));
    }

    @Test
    void containersCopyTheirChildren() {
        List<Element> children = new ArrayList<>(List.of(post));
        Group snapshot = new Group("snapshot", children);
        children.clear();
        assertEquals(10L, snapshot.accept(new EngagementVisitor()));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.children().clear());
    }
}
