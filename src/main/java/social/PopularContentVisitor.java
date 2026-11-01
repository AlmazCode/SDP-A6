package social;

import java.util.List;

public final class PopularContentVisitor implements Visitor<List<Element>> {
    private final int minimumLikes;

    public PopularContentVisitor(int minimumLikes) {
        this.minimumLikes = minimumLikes;
    }

    private List<Element> filterChildren(List<? extends Element> children) {
        return children.stream().flatMap(child -> child.accept(this).stream()).toList();
    }

    private List<Element> select(Element element, int likes) {
        return likes >= minimumLikes ? List.of(element) : List.of();
    }

    @Override
    public List<Element> visit(Community community) {
        return filterChildren(community.children());
    }

    @Override
    public List<Element> visit(Group group) {
        return filterChildren(group.children());
    }

    @Override
    public List<Element> visit(TextPost post) {
        return select(post, post.likes());
    }

    @Override
    public List<Element> visit(Comment comment) {
        return select(comment, comment.likes());
    }

    @Override
    public List<Element> visit(Image image) {
        return select(image, image.likes());
    }
}
