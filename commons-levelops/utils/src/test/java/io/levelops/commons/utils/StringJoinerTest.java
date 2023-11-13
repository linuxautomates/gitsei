package io.levelops.commons.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringJoinerTest {

    @Test
    public void dedupeAndJoin() {
        assertThat(StringJoiner.dedupeAndJoin(", ", "a", null, " b ", "", "a")).isEqualTo("a, b");
    }

    @Test
    public void prefixIfNotBlank() {
        assertThat(StringJoiner.prefixIfNotBlank("a", null)).isEqualTo(null);
        assertThat(StringJoiner.prefixIfNotBlank("a", "")).isEqualTo("");
        assertThat(StringJoiner.prefixIfNotBlank("a", " ")).isEqualTo(" ");
        assertThat(StringJoiner.prefixIfNotBlank("a", "b")).isEqualTo("ab");
    }
}