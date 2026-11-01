package social;

import java.util.Objects;

public record SocialEvent(EventType type, Group group, String detail, long value) {
    public SocialEvent {
        Objects.requireNonNull(type);
        Objects.requireNonNull(group);
        Objects.requireNonNull(detail);
    }
}
