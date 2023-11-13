package io.levelops.commons.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MapUtils {

    public static <K, V> Map<K, V> emptyIfNull(final Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    public static <K, V> boolean isEmpty(final Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static <K, V> int size(final Map<K, V> map) {
        return map == null ? 0 : map.size();
    }

    /**
     * Given a key, applies function to value of map and stores result back.
     * If there was no value for that key, creates it with defaultValue then applies function.
     * E.g.:
     * HashMap<String, Integer> map = new HashMap<>();
     * apply(map, "key", () -> 0, i -> i + 1);
     */
    public static <K, V> Map<K, V> apply(Map<K, V> map, K key, Supplier<V> defaultValue, Function<V, V> function) {
        V value = map.get(key);
        // lazily getting defaultValue by not using getOrDefault
        if (value == null) {
            value = defaultValue.get();
        }
        V newValue = function.apply(value);
        map.put(key, newValue);
        return map;
    }

    /**
     * Given a key, mutates value of map (if consumer has side effects).
     * If there was no value for that key, creates it with defaultValue then mutates it.
     * E.g.:
     * HashMap<String, List<String>> map = new HashMap<>();
     * mutate(map, "key", ArrayList::new, list -> list.add("value"));
     */
    public static <K, V> Map<K, V> mutate(Map<K, V> map, K key, Supplier<V> defaultValue, Consumer<V> valueMutator) {
        return apply(map, key, defaultValue, v -> {
            valueMutator.accept(v);
            return v;
        });
    }

    @Nonnull
    public static <K, V> Map<K, V> merge(@Nullable Map<K, V> a, @Nullable Map<K, V> b) {
        Map<K, V> map = new HashMap<>();
        map.putAll(emptyIfNull(a));
        map.putAll(emptyIfNull(b));
        return map;
    }

    @Nonnull
    public static <K, V> Map<K, V> append(@Nullable Map<K, V> a, K key, V value) {
        Map<K, V> map = new HashMap<>(emptyIfNull(a));
        if (key != null && value != null) {
            map.put(key, value);
        }
        return map;
    }

    public static <K, V> Map<K, V> filter(Map<K, V> map, Predicate<Map.Entry<K, V>> predicate) {
        return emptyIfNull(map).entrySet().stream()
                .filter(predicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <K, V> Map<K, V> transform(Map<K, V> map, Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper) {
        return emptyIfNull(map).entrySet().stream()
                .map(mapper)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a map with only the entries where the key is in the set of keys to extract.
     * If a key to extract is not in the original map, return defaultValue, or skips it if null.
     */
    public static <K, V> Map<K, V> extract(Map<K, V> map, Set<K> keysToExtract, @Nullable Function<K, V> defaultValue) {
        if (CollectionUtils.isEmpty(keysToExtract)) {
            return Collections.emptyMap();
        }
        Map<K, V> extracted = MapUtils.filter(map, kv -> keysToExtract.contains(kv.getKey()));
        if (defaultValue != null && extracted.size() != keysToExtract.size()) {
            SetUtils.difference(keysToExtract, extracted.keySet()).forEach(missingKey -> {
                V val = defaultValue.apply(missingKey);
                if (val != null) {
                    extracted.put(missingKey, val);
                }
            });
        }
        return extracted;
    }

    public static <K, V> Map<K, V> extract(Map<K, V> map, Set<K> keysToExtract) {
        return extract(map, keysToExtract, null);
    }
}
