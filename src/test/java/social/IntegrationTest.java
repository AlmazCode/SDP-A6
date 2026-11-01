package social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    @Test
    void postEventModeratesOnlyItsRelatedGroupAndPublishesReport() {
        Group related = new Group("java", List.of(new TextPost("p1", "hello", 1)));
        Group unrelated = new Group("music", List.of(new Comment("c1", "spam", 0)));
        Community community = new Community("students", List.of(related, unrelated));
        PostSource posts = new PostSource();
        EventSource reports = new EventSource();
        EventLog log = new EventLog();
        posts.subscribe(EventType.POST_CREATED, new ModerationService(reports));
        reports.subscribe(EventType.MODERATION_COMPLETED, log);
        posts.post(community.children().get(0), "p1");
        assertEquals(1, log.events().size());
        SocialEvent report = log.events().get(0);
        assertSame(related, report.group());
        assertEquals(EventType.MODERATION_COMPLETED, report.type());
        assertEquals(0, report.value());
        assertEquals("reviewed=1, flagged=[]", report.detail());
    }

    @Test
    void moderationReportContainsFlaggedContent() {
        Group group = new Group("java", List.of(new Image("i1", "spam offer", 3)));
        EventSource reports = new EventSource();
        EventLog log = new EventLog();
        reports.subscribe(EventType.MODERATION_COMPLETED, log);
        new ModerationService(reports).onEvent(new SocialEvent(
                EventType.POST_CREATED, group, "i1", 1));
        assertEquals(1, log.events().get(0).value());
        assertEquals("reviewed=1, flagged=[i1]", log.events().get(0).detail());
    }

    @Test
    @Timeout(15)
    void fourThreadsDeliverExactlyFourThousandEventsThroughTheChain() throws Exception {
        Group group = new Group("java", List.of());
        ReactionSource source = new ReactionSource();
        TrendingTracker trending = new TrendingTracker(List.of(group));
        NotificationCenter notifications = new NotificationCenter();
        EventLog log = new EventLog();
        source.subscribe(EventType.REACTION_ADDED, trending);
        source.subscribe(EventType.REACTION_ADDED, notifications);
        trending.subscribe(EventType.TREND_UPDATED, log);
        List<Runnable> jobs = new ArrayList<>();
        for (int worker = 0; worker < 4; worker++) {
            jobs.add(() -> {
                for (int event = 0; event < 1_000; event++) {
                    source.react(group, "p1");
                }
            });
        }
        var executor = Executors.newFixedThreadPool(4);
        new Producers(executor).run(jobs);
        assertEquals(4_000, notifications.count());
        assertEquals(4_000, trending.reactions("java"));
        assertEquals(4_000, log.events().size());
        assertEquals(4_000, log.events().stream().map(SocialEvent::value).distinct().count());
        assertTrue(executor.isTerminated());
    }

    @Test
    @Timeout(15)
    void gracefulShutdownFinishesAllAcceptedJobs() throws Exception {
        AtomicInteger completed = new AtomicInteger();
        List<Runnable> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            jobs.add(completed::incrementAndGet);
        }
        var executor = Executors.newFixedThreadPool(3);
        new Producers(executor).run(jobs);
        assertEquals(100, completed.get());
        assertTrue(executor.isTerminated());
    }

    @Test
    @Timeout(15)
    void failedProducerStillDrainsOtherJobsAndClosesExecutor() {
        AtomicInteger completed = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(3);
        assertThrows(java.util.concurrent.ExecutionException.class,
                () -> new Producers(executor).run(List.of(
                        () -> { throw new IllegalStateException("failure"); },
                        completed::incrementAndGet,
                        completed::incrementAndGet)));
        assertEquals(2, completed.get());
        assertTrue(executor.isTerminated());
    }
}
