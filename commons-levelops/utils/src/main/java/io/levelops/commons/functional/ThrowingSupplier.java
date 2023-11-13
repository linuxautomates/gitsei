package io.levelops.commons.functional;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

    T get() throws E;

}
