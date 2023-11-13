package io.levelops.commons.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.levelops.commons.utils.MapUtils.apply;
import static io.levelops.commons.utils.MapUtils.mutate;
import static org.assertj.core.api.Assertions.assertThat;

public class MapUtilsTest {

    @Test
    public void testApply() {
        HashMap<String, Integer> map = new HashMap<>();
        apply(map, "key", () -> 0, i -> i + 1);
        assertThat(map).containsKey("key");
        assertThat(map.get("key")).isEqualTo(1);
    }

    @Test
    public void testMutate() {
        HashMap<String, List<String>> map = new HashMap<>();
        mutate(map, "key", ArrayList::new, l -> l.add("value1"));
        mutate(map, "key", ArrayList::new, l -> l.add("value2"));
        assertThat(map).containsKey("key");
        assertThat(map.get("key")).containsExactly("value1", "value2");
    }

    @Test
    public void testFilter() {
        Map<String, String> filtered = MapUtils.filter(Map.of("1", "a", "2", "b", "3", "c"),
                kv -> Integer.parseInt(kv.getKey()) % 2 != 0);
        assertThat(filtered).containsExactlyInAnyOrderEntriesOf(Map.of("1", "a", "3", "c"));
    }

    @Test
    public void testExtract() {
        Map<String, String> out = MapUtils.extract(Map.of("1", "a", "2", "b", "3", "c"),
                Set.of("1", "3", "5"), null);
        assertThat(out).containsExactlyInAnyOrderEntriesOf(Map.of("1", "a", "3", "c"));

        out = MapUtils.extract(Map.of("1", "a", "2", "b", "3", "c"),
                Set.of("1", "3", "5"), k -> "-1");
        assertThat(out).containsExactlyInAnyOrderEntriesOf(Map.of("1", "a", "3", "c", "5", "-1"));
    }
}