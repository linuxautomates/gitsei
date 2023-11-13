package io.levelops.commons.exceptions;

import java.util.function.Function;

@FunctionalInterface
public interface FunctionWithException<T, R, E extends Exception> {

    R apply(T t) throws E;

    public static <T, R, E1 extends Exception, E2 extends RuntimeException> Function<T, R> wrap(FunctionWithException<T, R, E1> delegate,
                                                                                                ExceptionSuppliers.ExceptionWithCauseSupplier<E2> exceptionWithCauseSupplier) {
        return t -> {
            try {
                return delegate.apply(t);
            } catch (Exception e) {
                throw exceptionWithCauseSupplier.build(e);
            }
        };
    }

    public static <T, R, E1 extends Exception> Function<T, R> wrapAsRuntime(FunctionWithException<T, R, E1> delegate) {
        return wrap(delegate, ExceptionSuppliers.ExceptionWithCauseSupplier.forClass(RuntimeException.class));
    }

}
