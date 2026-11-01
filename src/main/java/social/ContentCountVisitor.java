package social;

import java.util.List;

public final class ContentCountVisitor implements Visitor<Integer> {
    private int count(List<? extends Element> children) {
        return children.stream().mapToInt(child -> child.accept(this)).sum();
    }

    @Override
    public Integer visit(Community community) {
        return count(community.children());
    }

    @Override
    public Integer visit(Group group) {
        return count(group.children());
    }

    @Override
    public Integer visit(TextPost post) {
        return 1;
    }

    @Override
    public Integer visit(Comment comment) {
        return 1;
    }

    @Override
    public Integer visit(Image image) {
        return 1;
    }
}
