package io.levelops.commons.exceptions;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ExceptionSuppliers {

    @FunctionalInterface
    interface ExceptionSupplier<E extends Exception> {
        E build();

        static <E extends Exception> ExceptionSupplier<E> forClass(Class<E> clazz) {
            expectClassWithConstructor(clazz);
            return () -> {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Could not use following class as Exception Supplier: " + clazz);
                }
            };
        }
    }

    @FunctionalInterface
    interface ExceptionWithMessageSupplier<E extends Exception> {
        E build(String msg);

        static <E extends Exception> ExceptionWithMessageSupplier<E> forClass(Class<E> clazz) {
            expectClassWithConstructor(clazz, String.class);
            return (String msg) -> {
                try {
                    return clazz.getConstructor(String.class).newInstance(msg);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Could not use following class as Exception Supplier: " + clazz);
                }
            };
        }
    }


    @FunctionalInterface
    interface ExceptionWithCauseSupplier<E extends Exception> {
        E build(Throwable cause);

        static <E extends Exception> ExceptionWithCauseSupplier<E> forClass(Class<E> clazz) {
            expectClassWithConstructor(clazz, Throwable.class);
            return (Throwable cause) -> {
                try {
                    return clazz.getConstructor(Throwable.class).newInstance(cause);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Could not use following class as Exception Supplier: " + clazz);
                }
            };
        }
    }

    @FunctionalInterface
    interface ExceptionWithCauseAndMessageSupplier<E extends Exception> {
        E build(String msg, Throwable cause);

        static <E extends Exception> ExceptionWithCauseAndMessageSupplier<E> forClass(Class<E> clazz) {
            expectClassWithConstructor(clazz, String.class, Throwable.class);
            return (String msg, Throwable cause) -> {
                try {
                    return clazz.getConstructor(String.class, Throwable.class).newInstance(msg, cause);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Could not use following class as Exception Supplier: " + clazz);
                }
            };
        }
    }

    static <E> void expectClassWithConstructor(Class<E> clazz, Class<?>... parameterTypes) {
        try {
            clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Expecting constructor: " + clazz.getSimpleName() + "("
                    + Stream.of(parameterTypes).map(Class::getSimpleName).collect(Collectors.joining(", "))
                    + ")");
        }
    }
}
