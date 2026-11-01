package social;

public interface Visitor<R> {
    R visit(Community community);

    R visit(Group group);

    R visit(TextPost post);

    R visit(Comment comment);

    R visit(Image image);
}
