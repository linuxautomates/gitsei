package io.levelops.commons.functional;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SupplierUtilsTest {

    @Test
    public void name() {

        assertThat(SupplierUtils.fromThrowingSupplier(() -> "abc").get()).isEqualTo("abc");
        assertThat(SupplierUtils.fromThrowingSupplier(() -> {throw new Exception();}).get()).isNull();

    }
}