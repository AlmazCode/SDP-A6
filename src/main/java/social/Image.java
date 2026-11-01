package social;

public record Image(String id, String caption, int likes) implements Element {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
