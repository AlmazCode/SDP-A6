package social;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class EventLog implements Observer<SocialEvent> {
    private final ConcurrentLinkedQueue<SocialEvent> events = new ConcurrentLinkedQueue<>();

    @Override
    public void onEvent(SocialEvent event) {
        events.add(event);
    }

    public List<SocialEvent> events() {
        return List.copyOf(events);
    }
}
