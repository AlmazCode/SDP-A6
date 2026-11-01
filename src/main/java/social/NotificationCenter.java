package social;

import java.util.concurrent.atomic.AtomicInteger;

public final class NotificationCenter implements Observer<SocialEvent> {
    private final AtomicInteger count = new AtomicInteger();

    @Override
    public void onEvent(SocialEvent event) {
        count.incrementAndGet();
    }

    public int count() {
        return count.get();
    }
}
