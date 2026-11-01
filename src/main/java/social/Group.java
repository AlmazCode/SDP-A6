package social;

import java.util.List;

public record Group(String id, List<Element> children) implements Element {
    public Group {
        children = List.copyOf(children);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
