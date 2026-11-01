package social;

public record Comment(String id, String text, int likes) implements Element {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
