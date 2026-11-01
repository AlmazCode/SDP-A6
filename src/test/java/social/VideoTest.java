package social;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VideoTest {
    private final Video video = new Video("v1", "Spam clip", 7, 30);
    private final Community tree = new Community("students", List.of(
            new Group("java", List.of(new TextPost("p1", "Hello", 10), video))));

    @Test
    void newElementParticipatesInEngagementCalculation() {
        assertEquals(17L, tree.accept(new EngagementVisitor()));
    }

    @Test
    void newElementParticipatesInStatefulModeration() {
        ModerationVisitor visitor = new ModerationVisitor();
        tree.accept(visitor);
        assertEquals(2, visitor.reviewed());
        assertEquals(List.of("v1"), visitor.flaggedIds());
    }

    @Test
    void newElementParticipatesInFiltering() {
        assertEquals(List.of(video), video.accept(new PopularContentVisitor(7)));
        assertEquals(List.of(), video.accept(new PopularContentVisitor(8)));
    }

    @Test
    void newElementExportsItsSpecificData() {
        assertEquals("{\"type\":\"video\",\"id\":\"v1\",\"text\":\"Spam clip\",\"likes\":7,\"durationSeconds\":30}",
                video.accept(new JsonVisitor()));
    }

    @Test
    void newElementParticipatesInTheNewOperation() {
        assertEquals(2, tree.accept(new ContentCountVisitor()));
    }
}
