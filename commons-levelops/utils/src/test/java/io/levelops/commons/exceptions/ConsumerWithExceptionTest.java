package io.levelops.commons.exceptions;

import org.junit.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConsumerWithExceptionTest {

    @Test
    public void wrap() {
        Consumer<String> consumer = ConsumerWithException.wrapAsRuntime(x -> {
            if (x.equals("throw")) {
                throw new IOException();
            }
        });
        assertThatThrownBy(() -> consumer.accept("throw")).isInstanceOf(RuntimeException.class);
        consumer.accept("x");
    }

}