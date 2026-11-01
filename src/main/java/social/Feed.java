package social;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Feed implements Observer<SocialEvent> {
    private final ConcurrentLinkedQueue<String> posts = new ConcurrentLinkedQueue<>();

    @Override
    public void onEvent(SocialEvent event) {
        posts.add(event.detail());
    }

    public List<String> posts() {
        return List.copyOf(posts);
    }
}
