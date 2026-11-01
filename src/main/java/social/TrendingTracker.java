package social;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class TrendingTracker extends EventSource implements Observer<SocialEvent> {
    private final Map<String, AtomicLong> reactions;

    public TrendingTracker(List<Group> groups) {
        Map<String, AtomicLong> counters = new HashMap<>();
        for (Group group : groups) {
            if (counters.put(group.id(), new AtomicLong()) != null) {
                throw new IllegalArgumentException("Duplicate group: " + group.id());
            }
        }
        reactions = Map.copyOf(counters);
    }

    @Override
    public void onEvent(SocialEvent event) {
        long total = reactions.get(event.group().id()).incrementAndGet();
        publish(new SocialEvent(EventType.TREND_UPDATED, event.group(), "reactions", total));
    }

    public long reactions(String groupId) {
        return reactions.get(groupId).get();
    }
}
