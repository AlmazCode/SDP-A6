package social;

public interface Element {
    String id();

    <R> R accept(Visitor<R> visitor);
}
