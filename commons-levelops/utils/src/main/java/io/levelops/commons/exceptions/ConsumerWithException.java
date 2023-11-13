package io.levelops.commons.exceptions;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerWithException<T, E extends Exception> {

    void accept(T t) throws E;

    static <T, E1 extends Exception, E2 extends RuntimeException> Consumer<T> wrap(ConsumerWithException<T, E1> delegate,
                                                                                   ExceptionSuppliers.ExceptionWithCauseSupplier<E2> exceptionWithCauseSupplier) {
        return t -> {
            try {
                delegate.accept(t);
            } catch (Exception e) {
                throw exceptionWithCauseSupplier.build(e);
            }
        };
    }

    public static <T, E1 extends Exception> Consumer<T> wrapAsRuntime(ConsumerWithException<T, E1> delegate) {
        return ConsumerWithException.wrap(delegate, ExceptionSuppliers.ExceptionWithCauseSupplier.forClass(RuntimeException.class));
    }
}
