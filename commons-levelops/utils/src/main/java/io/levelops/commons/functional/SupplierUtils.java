package io.levelops.commons.functional;

import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.function.Supplier;

@Log4j2
public class SupplierUtils {

    public static <T> Supplier<Optional<T>> fromThrowingSupplierToOptional(ThrowingSupplier<T, ? extends Exception> delegate) {
        return () -> Optional.ofNullable(fromThrowingSupplier(delegate).get());
    }

    public static <T> Supplier<T> fromThrowingSupplier(ThrowingSupplier<T, ? extends Exception> delegate) {
        return () -> {
            try {
                return delegate.get();
            } catch (Exception e) {
                log.warn("Exception was thrown by supplier", e);
                return null;
            }
        };
    }

}
