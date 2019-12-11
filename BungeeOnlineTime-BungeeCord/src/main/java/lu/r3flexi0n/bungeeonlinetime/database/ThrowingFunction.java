package lu.r3flexi0n.bungeeonlinetime.database;

@FunctionalInterface
public interface ThrowingFunction<S, R, T extends Throwable> {

    R apply(S s) throws T;
}
