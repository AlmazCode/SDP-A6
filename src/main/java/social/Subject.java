package social;

public interface Subject<E> {
    void subscribe(EventType type, Observer<E> observer);

    void unsubscribe(EventType type, Observer<E> observer);

    void publish(E event);
}
