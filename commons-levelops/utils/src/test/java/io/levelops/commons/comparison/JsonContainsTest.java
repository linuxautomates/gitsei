package io.levelops.commons.comparison;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.comparison.JsonContains.contains;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonContainsTest {

    @Test
    public void testNull() {
        assertThat(contains(null, "a")).isFalse();
        assertThat(contains("a", null)).isTrue();
    }

    @Test
    public void testContainsString() {
        // str
        assertThat(contains("a", "a")).isTrue();
        assertThat(contains(123, 123)).isTrue();
        assertThat(contains("123", 123)).isTrue();
        assertThat(contains(123, "123")).isTrue();
        assertThat(contains("b", "a")).isFalse();

        // list
        assertThat(contains(List.of("a", "b"), "a")).isTrue();
        assertThat(contains(List.of("a", "b"), "c")).isFalse();
        assertThat(contains(List.of(List.of("a")), "a")).isTrue();
        assertThat(contains(List.of(List.of(123)), "123")).isTrue();

        // map
        assertThat(contains(Map.of("a", "b"), "a")).isFalse();
    }

    @Test
    public void testContainsList() {
        // str
        assertThat(contains("a", List.of("a"))).isTrue();
        assertThat(contains("a", List.of("b"))).isFalse();
        assertThat(contains("a", List.of("a", "b"))).isFalse();
        assertThat(contains("a", List.of(Map.of("a", "b")))).isFalse();
        assertThat(contains("a", List.of("a", "a", "a"))).isTrue();

        // list
        assertThat(contains(List.of("a", "b"), List.of("a"))).isTrue();
        assertThat(contains(List.of("a", "b"), List.of("c"))).isFalse();
        assertThat(contains(List.of(List.of("a")), List.of("a"))).isTrue();

        // map
        assertThat(contains(Map.of("a", "b"), List.of("a"))).isFalse();
        assertThat(contains(Map.of("a", "b"), List.of(Map.of("a", "b")))).isTrue();
        assertThat(contains(Map.of("a", "b", "c", "d"), List.of(Map.of("a", "b")))).isTrue();
        assertThat(contains(Map.of("a", "b", "c", "d"), List.of(Map.of("a", "b"), Map.of("c", "d")))).isTrue();
    }

    @Test
    public void testContainsMap() {
        // str
        assertThat(contains("a", Map.of("a", "b"))).isFalse();

        // list
        assertThat(contains(List.of("a", "b"), Map.of("a", "b"))).isFalse();
        assertThat(contains(List.of(Map.of("a", "b"), Map.of("c", "d")), Map.of("a", "b"))).isTrue();

        // map
        assertThat(contains(Map.of("a", "b"), Map.of("a", "b"))).isTrue();
        assertThat(contains(Map.of("a", "b", "c", "d"), Map.of("a", "b"))).isTrue();
        assertThat(contains(Map.of("c", "d"), Map.of("a", "b"))).isFalse();

        assertThat(contains(Map.of("c", Map.of("a", List.of("b", "b2"), "c", "d")), Map.of("c", Map.of("a", "b")))).isTrue();
        assertThat(contains(Map.of("c", Map.of("a", List.of("b", "b2"), "c", "d")), Map.of("c", Map.of("a", "b", "c", "d")))).isTrue();

    }

    @Test
    public void testUseCase() {
        Map<String, Object> eventData = Map.of(
                "id", "123",
                "fields", List.of(
                        Map.of("k", "a", "v", "b"),
                        Map.of("k", "c", "v", "d")
                )
        );
        assertThat(contains(eventData, Map.of("id", "123"))).isTrue();
        assertThat(contains(eventData, Map.of("fields", List.of(Map.of("k", "a"))))).isTrue();
        assertThat(contains(eventData, Map.of("fields", List.of(Map.of("v", "b"))))).isTrue();
        assertThat(contains(eventData, Map.of("fields", List.of(Map.of("k", "a", "v", "b"))))).isTrue();
        assertThat(contains(eventData, Map.of("id", "123", "fields", List.of(Map.of("k", "c"))))).isTrue();

        assertThat(contains(eventData, Map.of("id", "0"))).isFalse();
        assertThat(contains(eventData, Map.of("fields", List.of(Map.of("k", "???"))))).isFalse();
        assertThat(contains(eventData, Map.of("id", "0", "fields", List.of(Map.of("k", "c"))))).isFalse();
        assertThat(contains(eventData, Map.of("id", "123", "fields", List.of(Map.of("k", "???"))))).isFalse();
    }


    @Test
    public void testEmpty() {
        // str contained by str
        assertThat(contains("b", "")).isFalse();
        // str contained by list
        assertThat(contains(List.of(""), "")).isTrue();
        // str contained by map
        assertThat(contains(Map.of("a", "b"), "")).isFalse();

        // list contained by str
        assertThat(contains("a", List.of(""))).isFalse();
        // list contained by list
        assertThat(contains(List.of("a"), List.of(""))).isFalse();
        // list contained by map
        assertThat(contains(Map.of("a", "b"), List.of(""))).isFalse();
    }

    @Test
    public void testEmptyIgnoreEmptyStrings() {
        // str contained by str
        assertThat(contains("b", "", JsonContains.SearchOptions.IGNORE_EMPTY_STRINGS)).isTrue();
        // str contained by list
        assertThat(contains(List.of(""), "", JsonContains.SearchOptions.IGNORE_EMPTY_STRINGS)).isTrue();
        // str contained by map
        assertThat(contains(Map.of("a", "b"), "", JsonContains.SearchOptions.IGNORE_EMPTY_STRINGS)).isTrue();

        // list contained by str
        assertThat(contains("a", List.of(""), JsonContains.SearchOptions.IGNORE_EMPTY_STRINGS)).isTrue();
        // list contained by list
        assertThat(contains(List.of("a"), List.of(""), JsonContains.SearchOptions.IGNORE_EMPTY_STRINGS)).isTrue();
        // list contained by map
        assertThat(contains(Map.of("a", "b"), List.of(""), JsonContains.SearchOptions.IGNORE_EMPTY_STRINGS)).isTrue();
    }

    @Test
    public void testEmptyTrimAndIgnoreEmptyStrings() {
        // str contained by str
        assertThat(contains("b", " ", JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        assertThat(contains("b", " b ", JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        // str contained by list
        assertThat(contains(List.of(""), " ", JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        assertThat(contains(List.of("a"), " a ", JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        // str contained by map
        assertThat(contains(Map.of("a", "b"), " ", JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        assertThat(contains(Map.of("a", "b"), " b ", JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isFalse();

        // list contained by str
        assertThat(contains("a", List.of(" "), JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        assertThat(contains("a", List.of(" a "), JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        // list contained by list
        assertThat(contains(List.of("a"), List.of(" "), JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        assertThat(contains(List.of("a"), List.of(" a "), JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        // list contained by map
        assertThat(contains(Map.of("a", "b"), List.of(" "), JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isTrue();
        assertThat(contains(Map.of("a", "b"), List.of(" b "), JsonContains.SearchOptions.TRIM_AND_IGNORE_EMPTY_STRINGS)).isFalse();
    }
}