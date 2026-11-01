package social;

public final class PostSource extends EventSource {
    public void post(Group group, String postId) {
        publish(new SocialEvent(EventType.POST_CREATED, group, postId, 1));
    }
}
