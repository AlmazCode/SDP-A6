package social;

public final class ReactionSource extends EventSource {
    public void react(Group group, String contentId) {
        publish(new SocialEvent(EventType.REACTION_ADDED, group, contentId, 1));
    }
}
