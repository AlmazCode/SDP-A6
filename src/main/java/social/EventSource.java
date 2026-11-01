package social;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventSource implements Subject<SocialEvent> {
    private final Map<EventType, CopyOnWriteArrayList<Observer<SocialEvent>>> listeners;

    public EventSource() {
        listeners = new EnumMap<>(EventType.class);
        for (EventType type : EventType.values()) {
            listeners.put(type, new CopyOnWriteArrayList<>());
        }
    }

    @Override
    public void subscribe(EventType type, Observer<SocialEvent> observer) {
        listeners.get(type).addIfAbsent(Objects.requireNonNull(observer));
    }

    @Override
    public void unsubscribe(EventType type, Observer<SocialEvent> observer) {
        listeners.get(type).remove(observer);
    }

    @Override
    public void publish(SocialEvent event) {
        RuntimeException failure = null;
        // Each iterator sees a snapshot, so callbacks may change subscriptions.
        for (Observer<SocialEvent> observer : listeners.get(event.type())) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Listener failed", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
