package io.levelops.commons.jackson;

import com.fasterxml.jackson.databind.MappingIterator;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CsvParsingTest {

    @Test
    public void test() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("a, b, c\nc,d\ne,f,g ".getBytes());
        MappingIterator<Map<String, String>> it = CsvParsing.parse(in);
        List<String> cols = CsvParsing.extractColumnsFromHeader(it);
        assertThat(cols).containsExactly("a", "b", "c");

        List<Map<String, String>> rows = it.readAll();
        assertThat(rows).containsExactly(
                Map.of(
                        "a", "c",
                        "b", "d"),
                Map.of(
                        "a", "e",
                        "b", "f",
                        "c", "g "));
    }
}