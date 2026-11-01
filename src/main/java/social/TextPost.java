package social;

public record TextPost(String id, String text, int likes) implements Element {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
