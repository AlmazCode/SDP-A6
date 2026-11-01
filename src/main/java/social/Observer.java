package social;

@FunctionalInterface
public interface Observer<E> {
    void onEvent(E event);
}
