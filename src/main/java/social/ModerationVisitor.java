package social;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModerationVisitor implements Visitor<Void> {
    private int reviewed;
    private final List<String> flaggedIds = new ArrayList<>();

    private Void inspect(String id, String text) {
        reviewed++;
        if (text.toLowerCase(Locale.ROOT).contains("spam")) {
            flaggedIds.add(id);
        }
        return null;
    }

    private Void inspectChildren(List<? extends Element> children) {
        for (Element child : children) {
            child.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Community community) {
        return inspectChildren(community.children());
    }

    @Override
    public Void visit(Group group) {
        return inspectChildren(group.children());
    }

    @Override
    public Void visit(TextPost post) {
        return inspect(post.id(), post.text());
    }

    @Override
    public Void visit(Comment comment) {
        return inspect(comment.id(), comment.text());
    }

    @Override
    public Void visit(Image image) {
        return inspect(image.id(), image.caption());
    }

    public int reviewed() {
        return reviewed;
    }

    public List<String> flaggedIds() {
        return List.copyOf(flaggedIds);
    }
}
