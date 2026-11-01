package social;

import java.util.List;

public final class EngagementVisitor implements Visitor<Long> {
    private long total(List<? extends Element> children) {
        return children.stream().mapToLong(child -> child.accept(this)).sum();
    }

    @Override
    public Long visit(Community community) {
        return total(community.children());
    }

    @Override
    public Long visit(Group group) {
        return total(group.children());
    }

    @Override
    public Long visit(TextPost post) {
        return (long) post.likes();
    }

    @Override
    public Long visit(Comment comment) {
        return (long) comment.likes();
    }

    @Override
    public Long visit(Image image) {
        return (long) image.likes();
    }
}
