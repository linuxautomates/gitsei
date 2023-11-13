package io.levelops.commons.caching;

import io.levelops.commons.caching.CacheHashUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheHashUtilsTest {

    @Test
    public void generateCacheHash() {
        Assertions.assertThat(CacheHashUtils.generateCacheHash("{c=d, a=b}")).isEqualTo("7341f78dfc8d73bbfd32c6dddd4f2cd3c88ed2838cf3d2ddc2b9ddaa2ef5f7d7");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void generateCacheHashUsingToString() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "b");
        map.put("c", "d");
        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("c", "d");
        map2.put("a", "b");
        Map<String, Object> map3 = new LinkedHashMap<>();
        map3.put("a", "b");
        map3.put("c", "e");

        Assertions.assertThat(CacheHashUtils.generateCacheHashUsingToString(map)).isEqualTo("3843d44a2fdf90bec3150aed35a85fc227f9b13819a5ae86f8404e83e80ff2de");
        Assertions.assertThat(CacheHashUtils.generateCacheHashUsingToString(map2)).isEqualTo("7341f78dfc8d73bbfd32c6dddd4f2cd3c88ed2838cf3d2ddc2b9ddaa2ef5f7d7");
        Assertions.assertThat(CacheHashUtils.generateCacheHashUsingToString(map3)).isEqualTo("1308f7b82ccf2fae7ae536da91f699d17cce195cd18561a9e56f46b8813321e1");
    }

    @Test
    public void generateCacheHashUsingSerialization() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("c", "d");
        item.put("a", "b");
        item.put("0", "1");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "b");
        map.put("l", List.of(item));
        map.put("c", "d");
        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("c", "d");
        map2.put("l", List.of(item));
        map2.put("a", "b");

        String s = CacheHashUtils.generateCacheHashUsingSerialization(map);
        String s2 = CacheHashUtils.generateCacheHashUsingSerialization(map2);

        Assertions.assertThat(s).isEqualTo(s2);
        Assertions.assertThat(s).isEqualTo("eea8300d22a33489bdd4dd73668b32e214ff340e544223e7165338d708703e88");
    }
}