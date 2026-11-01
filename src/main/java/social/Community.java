package social;

import java.util.List;

public record Community(String id, List<Group> children) implements Element {
    public Community {
        children = List.copyOf(children);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
