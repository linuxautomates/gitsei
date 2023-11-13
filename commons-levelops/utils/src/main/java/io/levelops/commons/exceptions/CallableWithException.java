package io.levelops.commons.exceptions;

import java.util.concurrent.Callable;

public interface CallableWithException<V, E extends Exception> extends Callable<V> {

    @Override
    V call() throws E;

}
