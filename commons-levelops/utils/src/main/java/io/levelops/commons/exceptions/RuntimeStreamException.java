package io.levelops.commons.exceptions;

import java.util.function.Consumer;
import java.util.function.Function;

public class RuntimeStreamException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RuntimeStreamException() {
        super();
    }

    public RuntimeStreamException(final String message) {
        super(message);
    }

    public RuntimeStreamException(final String message, final Throwable t) {
        super(message, t);
    }

    public RuntimeStreamException(final Throwable t) {
        super(t);
    }

    public static <T, R, E1 extends Exception> Function<T, R> wrap(FunctionWithException<T, R, E1> delegate) {
        return FunctionWithException.wrap(delegate, ExceptionSuppliers.ExceptionWithCauseSupplier.forClass(RuntimeStreamException.class));
    }
}