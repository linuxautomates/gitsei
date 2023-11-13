package io.levelops.commons.utils;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ListUtilsTest {

    @Test
    public void isEmpty() {
        assertThat(ListUtils.isEmpty(null)).isTrue();
        assertThat(ListUtils.isEmpty(List.of())).isTrue();
        assertThat(ListUtils.isEmpty(List.of("a"))).isFalse();
    }

    @Test
    public void emptyIfNull() {
        assertThat(ListUtils.emptyIfNull(null)).isNotNull();
        assertThat(ListUtils.emptyIfNull(List.of())).isNotNull();
    }

    @Test
    public void addIfNotPresent() {
        assertThat(ListUtils.addIfNotPresent(null, "a")).containsExactly("a");
        assertThat(ListUtils.addIfNotPresent(List.of(), "a")).containsExactly("a");
        assertThat(ListUtils.addIfNotPresent(List.of(), null)).isNotNull().isEmpty();
        assertThat(ListUtils.addIfNotPresent(List.of("x"), "a")).containsExactly("x", "a");
        assertThat(ListUtils.addIfNotPresent(List.of("a"), "a")).containsExactly("a");
    }

    @Test
    public void intersection() {
        assertThat(ListUtils.intersection(null, null)).isNotNull().isEmpty();
        assertThat(ListUtils.intersection(List.of("a"), null)).isEmpty();
        assertThat(ListUtils.intersection(List.of("a"), List.of("a", "b"))).containsExactly("a");
    }

    @Test
    public void intersectionIgnoringEmpty() {
        assertThat(ListUtils.intersectionIgnoringEmpty(null, null)).isNotNull().isEmpty();
        assertThat(ListUtils.intersectionIgnoringEmpty(List.of("a"), null)).containsExactly("a");
        assertThat(ListUtils.intersectionIgnoringEmpty(null, List.of("a"))).containsExactly("a");
        assertThat(ListUtils.intersectionIgnoringEmpty(List.of("a"), List.of("a", "b"))).containsExactly("a");
    }

    @Test
    public void stream() {
        assertThat(ListUtils.stream(null)).isEmpty();
        assertThat(ListUtils.stream(List.of("a", "b"))).containsExactly("a", "b");
    }
}