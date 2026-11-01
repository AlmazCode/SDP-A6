package social;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObserverTest {
    private final Group group = new Group("java", List.of());

    @Test
    void deliversOnlySubscribedEventTypes() {
        EventSource source = new EventSource();
        AtomicInteger count = new AtomicInteger();
        source.subscribe(EventType.POST_CREATED, event -> count.incrementAndGet());
        source.publish(new SocialEvent(EventType.REACTION_ADDED, group, "p1", 1));
        source.publish(new SocialEvent(EventType.POST_CREATED, group, "p1", 1));
        assertEquals(1, count.get());
    }

    @Test
    void duplicateSubscriptionDeliversOnce() {
        EventSource source = new EventSource();
        AtomicInteger count = new AtomicInteger();
        Observer<SocialEvent> observer = event -> count.incrementAndGet();
        source.subscribe(EventType.POST_CREATED, observer);
        source.subscribe(EventType.POST_CREATED, observer);
        source.publish(new SocialEvent(EventType.POST_CREATED, group, "p1", 1));
        assertEquals(1, count.get());
    }

    @Test
    void canUnsubscribeAndSubscribeAgain() {
        PostSource source = new PostSource();
        Feed feed = new Feed();
        source.subscribe(EventType.POST_CREATED, feed);
        source.unsubscribe(EventType.POST_CREATED, feed);
        source.post(group, "hidden");
        source.subscribe(EventType.POST_CREATED, feed);
        source.post(group, "visible");
        assertEquals(List.of("visible"), feed.posts());
    }

    @Test
    void observerCanUnsubscribeItself() {
        PostSource source = new PostSource();
        AtomicInteger count = new AtomicInteger();
        Observer<SocialEvent> once = new Observer<>() {
            public void onEvent(SocialEvent event) {
                count.incrementAndGet();
                source.unsubscribe(EventType.POST_CREATED, this);
            }
        };
        source.subscribe(EventType.POST_CREATED, once);
        source.post(group, "first");
        source.post(group, "second");
        assertEquals(1, count.get());
    }

    @Test
    void subscriptionInsideHandlerStartsWithNextEvent() {
        PostSource source = new PostSource();
        AtomicInteger count = new AtomicInteger();
        Observer<SocialEvent> late = event -> count.incrementAndGet();
        source.subscribe(EventType.POST_CREATED,
                event -> source.subscribe(EventType.POST_CREATED, late));
        source.post(group, "first");
        assertEquals(0, count.get());
        source.post(group, "second");
        assertEquals(1, count.get());
    }

    @Test
    void removalInsideHandlerDoesNotChangeCurrentSnapshot() {
        PostSource source = new PostSource();
        Feed feed = new Feed();
        source.subscribe(EventType.POST_CREATED,
                event -> source.unsubscribe(EventType.POST_CREATED, feed));
        source.subscribe(EventType.POST_CREATED, feed);
        source.post(group, "first");
        source.post(group, "second");
        assertEquals(List.of("first"), feed.posts());
    }

    @Test
    void failingObserverDoesNotPreventOtherDeliveries() {
        PostSource source = new PostSource();
        Feed feed = new Feed();
        source.subscribe(EventType.POST_CREATED, event -> {
            throw new IllegalStateException("broken listener");
        });
        source.subscribe(EventType.POST_CREATED, feed);
        assertThrows(IllegalStateException.class, () -> source.post(group, "p1"));
        assertEquals(List.of("p1"), feed.posts());
    }

    @Test
    void independentSourcesNotifyTheSameListener() {
        PostSource posts = new PostSource();
        ReactionSource reactions = new ReactionSource();
        NotificationCenter notifications = new NotificationCenter();
        posts.subscribe(EventType.POST_CREATED, notifications);
        reactions.subscribe(EventType.REACTION_ADDED, notifications);
        posts.post(group, "p1");
        reactions.react(group, "p1");
        assertEquals(2, notifications.count());
    }

    @Test
    void trendingRepublishesPerGroupReactionTotals() {
        Group other = new Group("other", List.of());
        ReactionSource reactions = new ReactionSource();
        TrendingTracker trending = new TrendingTracker(List.of(group, other));
        EventLog log = new EventLog();
        reactions.subscribe(EventType.REACTION_ADDED, trending);
        trending.subscribe(EventType.TREND_UPDATED, log);
        reactions.react(group, "p1");
        reactions.react(other, "p2");
        reactions.react(group, "p1");
        assertEquals(List.of(1L, 1L, 2L),
                log.events().stream().map(SocialEvent::value).toList());
        assertEquals(2, trending.reactions("java"));
        assertEquals(1, trending.reactions("other"));
    }
}
