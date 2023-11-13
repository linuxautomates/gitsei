package io.levelops.commons.databases.models.database.cicd;

import org.junit.Test;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class PathSegmentTest {

    private static PathSegment ps(Integer pos) {
        return ps(pos, "#" + Objects.toString(pos));
    }

    private static PathSegment ps(Integer pos, String name) {
        return ps(null, pos, name);
    }

    private static PathSegment ps(String id, Integer pos, String name) {
        return PathSegment.builder().id(id).position(pos).name(name).build();
    }

    @Test
    public void testToStream() {
        assertThat(PathSegment.toStream(Set.of(ps(1), ps(3), ps(2), ps(null)), false))
                .containsExactly(ps(1, "#1"), ps(2, "#2"), ps(3, "#3"), ps(null, "#null"));

        assertThat(PathSegment.toStream(Set.of(ps(1), ps(3), ps(2), ps(null)), true))
                .containsExactly(ps(3, "#3"), ps(2, "#2"), ps(1, "#1"), ps(null, "#null"));
    }

    @Test
    public void testReindex() {
        assertThat(PathSegment.reindex(Stream.of(ps(1), ps(null), ps(3), ps(2), ps(null))))
                .containsExactlyInAnyOrder(ps(1, "#1"), ps(2, "#null"), ps(3, "#3"), ps(4, "#2"), ps(5, "#null"));
    }

    @Test
    public void testReverse() {
        assertThat(PathSegment.reverse(Set.of(ps(1), ps(3), ps(2), ps(null))))
                .containsExactlyInAnyOrder(ps(1, "#3"), ps(2, "#2"), ps(3, "#1"), ps(4, "#null"));
    }

    @Test
    public void testAppend() {
        assertThat(PathSegment.concat(Set.of(ps(3, "B"), ps(1, "A")), Set.of(ps(20, "C"), ps(null, "D"))))
                .containsExactlyInAnyOrder(ps(1, "A"), ps(2, "B"), ps(3, "C"), ps(4, "D"));
    }

    @Test
    public void testRemoveAll() {
        assertThat(PathSegment.removeAll(Set.of(ps("b", 3, "B"), ps("a", 1, "A"), ps("c", 20, "C")), Set.of(ps("b", null, "???"))))
                .containsExactlyInAnyOrder(ps("a", 1, "A"), ps("c", 2, "C"));
    }
}