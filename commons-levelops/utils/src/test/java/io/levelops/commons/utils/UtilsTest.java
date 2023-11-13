package io.levelops.commons.utils;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class UtilsTest {

    @Test
    public void testCommaListSplitterSplit() {
        List<String> split = CommaListSplitter.split("A,B,C,D");
        Assertions.assertThat(split).isEqualTo(List.of("A","B","C","D"));
    }

    @Test
    public void testIterableUtilsGetFirst() {
        Optional<String> split = IterableUtils.getFirst(List.of("A","B","C","D"));
        Assertions.assertThat(split.get()).isEqualTo("A");
    }

    @Test
    public void testIterableUtilsGetLast() {
        Optional<String> split = IterableUtils.getLast(List.of("A","B","C","D"));
        Assertions.assertThat(split.get()).isEqualTo("D");
    }
}
