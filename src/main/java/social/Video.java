package social;

public record Video(String id, String caption, int likes, int durationSeconds) implements Element {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
