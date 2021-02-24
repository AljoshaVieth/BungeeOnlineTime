package lu.r3flexi0n.bungeeonlinetime.database;

@FunctionalInterface
public interface ThrowingConsumer<E, T extends Throwable> {

    static <E, T extends Throwable> ThrowingConsumer<E, T> nothing() {
        return element -> {};
    }

    void accept(E element) throws T;
}
