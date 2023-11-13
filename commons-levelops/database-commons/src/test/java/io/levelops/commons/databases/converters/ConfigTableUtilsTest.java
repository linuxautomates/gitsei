package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.jackson.CsvParsing;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigTableUtilsTest {

    @Test
    public void test() throws IOException {
        Stream<Map<String, String>> csvData = CsvParsing.parseToStream(new ByteArrayInputStream("a,b\nc,d\ne,f".getBytes()));
        ConfigTable configTable = ConfigTableUtils.fromCsv(csvData, null, true);
        DefaultObjectMapper.prettyPrint(configTable);
    }

    @Test
    public void testFindMaxIndex() {
        assertThat(ConfigTableUtils.findMaxColumnIndex(null)).isEqualTo(0);
        assertThat(ConfigTableUtils.findMaxColumnIndex(Map.of(
                "1", ConfigTable.Column.builder().index(1).build(),
                "2", ConfigTable.Column.builder().index(20).build(),
                "3", ConfigTable.Column.builder().index(null).build(),
                "4", ConfigTable.Column.builder().index(10).build(),
                "5", ConfigTable.Column.builder().index(-1).build()))
        ).isEqualTo(20);
    }

    @Test
    public void testSanitizeColumn() {
        assertThatThrownBy(() -> ConfigTableUtils.sanitizeColumn("1", ConfigTable.Column.builder().build(), new MutableInt(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id mismatch");
        assertThatThrownBy(() -> ConfigTableUtils.sanitizeColumn("1", ConfigTable.Column.builder().id("2").build(), new MutableInt(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id mismatch");

        assertThatThrownBy(() -> ConfigTableUtils.sanitizeColumn("1", ConfigTable.Column.builder().id("1").build(), new MutableInt(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Column name cannot be empty");

        ConfigTable.Column c = ConfigTableUtils.sanitizeColumn("1", ConfigTable.Column.builder().id("1").displayName(" Col 1 a!-2b ").build(), new MutableInt(0));
        assertThat(c.getId()).isEqualTo("1");
        assertThat(c.getDisplayName()).isEqualTo("Col 1 a!-2b");
        assertThat(c.getKey()).isEqualTo("col_1_a__2b");
    }

    @Test
    public void sanitizeSchema() {
        ConfigTable out = ConfigTableUtils.sanitize(ConfigTable.builder()
                .schema(ConfigTable.Schema.builder()
                        .columns(Map.of(
                                "1", ConfigTable.Column.builder().id("1").displayName("a").build(),
                                "2", ConfigTable.Column.builder().id("2").displayName("b").build()))
                        .build())
                .build());
        assertThat(out.getSchema().getColumns()).hasSize(2);
        assertThat(out.getSchema().getColumns().values().stream().map(ConfigTable.Column::getIndex).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1, 2);

        assertThatThrownBy(() -> ConfigTableUtils.sanitize(ConfigTable.builder()
                .schema(ConfigTable.Schema.builder()
                        .columns(Map.of(
                                "1", ConfigTable.Column.builder().id("1").displayName("a").build(),
                                "2", ConfigTable.Column.builder().id("2").displayName("a").build()))
                        .build())
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Column keys must be unique");
    }

    @Test
    public void testDiff() {
        ConfigTable t1 = ConfigTable.builder()
                .schema(ConfigTable.Schema.builder()
                        .columns(Map.of(
                                "c1", ConfigTable.Column.builder()
                                        .id("c1")
                                        .displayName("key column")
                                        .build(),
                                "c2", ConfigTable.Column.builder()
                                        .id("c2")
                                        .displayName("column A")
                                        .build(),
                                "c3", ConfigTable.Column.builder()
                                        .id("c3")
                                        .displayName("column B")
                                        .build(),
                                "c4", ConfigTable.Column.builder()
                                        .id("c4")
                                        .displayName("column C")
                                        .build()
                        ))
                        .build())
                .rows(Map.of(
                        "0", ConfigTable.Row.builder()
                                .id("r1")
                                .values(Map.of("c1", "key0", "c2", "valueA0"))
                                .build(),
                        "1", ConfigTable.Row.builder()
                                .id("r2")
                                .values(Map.of("c1", "key1", "c2", "valueA1", "c3", "valueB1"))
                                .build(),
                        "2", ConfigTable.Row.builder()
                                .id("r3")
                                .values(Map.of("c1", "key3", "c4", "valueC3"))
                                .build(),
                        "3", ConfigTable.Row.builder()
                                .id("r4")
                                .values(Map.of("c1", "key4"))
                                .build()
                ))
                .build();
        ConfigTable t2 = ConfigTable.builder()
                .schema(ConfigTable.Schema.builder()
                        .columns(Map.of(
                                "c10", ConfigTable.Column.builder()
                                        .id("c10")
                                        .displayName("key column")
                                        .build(),
                                "c0", ConfigTable.Column.builder()
                                        .id("c0")
                                        .displayName("column A")
                                        .build(),
                                "c1", ConfigTable.Column.builder()
                                        .id("c1")
                                        .displayName("column B")
                                        .build(),
                                "c2", ConfigTable.Column.builder()
                                        .id("c2")
                                        .displayName("column C")
                                        .build()
                        ))
                        .build())
                .rows(Map.of(
                        "1", ConfigTable.Row.builder()
                                .id("r1")
                                .values(Map.of("c10", "key1", "c0", "valueA1-changed","c1", "valueB1"))
                                .build(),
                        "2", ConfigTable.Row.builder()
                                .id("r2")
                                .values(Map.of("c10", "key2", "c0", "valueA2"))
                                .build(),
                        "3", ConfigTable.Row.builder()
                                .id("r3")
                                .values(Map.of("c10", "key3"))
                                .build(),
                        "4", ConfigTable.Row.builder()
                                .id("r4")
                                .values(Map.of("c10", "key4",  "c2", "valueC4"))
                                .build()
                ))
                .build();
        ConfigTableUtils.CsvDiffResult diff = ConfigTableUtils.diffByUniqueKey(t1, t2, "key column", Set.of("column A", "column B"));
        DefaultObjectMapper.prettyPrint(diff);
        assertThat(diff.getAddedKeys()).containsExactlyInAnyOrder("key2");
        assertThat(diff.getRemovedKeys()).containsExactlyInAnyOrder("key0");
        assertThat(diff.getChangedKeys()).containsExactlyInAnyOrder("key1");

        diff = ConfigTableUtils.diffByUniqueKey(t1, t2, "key column", Set.of("column A", "column B", "column C"));
        DefaultObjectMapper.prettyPrint(diff);
        assertThat(diff.getAddedKeys()).containsExactlyInAnyOrder("key2");
        assertThat(diff.getRemovedKeys()).containsExactlyInAnyOrder("key0");
        assertThat(diff.getChangedKeys()).containsExactlyInAnyOrder("key1", "key3", "key4");
    }

    @Test
    public void testGetRowsByUniqueKey() {
        ConfigTable t1 = ConfigTable.builder()
                .schema(ConfigTable.Schema.builder()
                        .columns(Map.of(
                                "c1", ConfigTable.Column.builder()
                                        .id("c1")
                                        .displayName("key column")
                                        .build(),
                                "c2", ConfigTable.Column.builder()
                                        .id("c2")
                                        .displayName("column A")
                                        .build(),
                                "c3", ConfigTable.Column.builder()
                                        .id("c3")
                                        .displayName("column B")
                                        .build(),
                                "c4", ConfigTable.Column.builder()
                                        .id("c4")
                                        .displayName("column C")
                                        .build()
                        ))
                        .build())
                .rows(Map.of(
                        "0", ConfigTable.Row.builder()
                                .id("r1")
                                .values(Map.of("c1", "key0", "c2", "valueA0"))
                                .build(),
                        "1", ConfigTable.Row.builder()
                                .id("r2")
                                .values(Map.of("c1", "key1", "c2", "valueA1", "c3", "valueB1"))
                                .build(),
                        "2", ConfigTable.Row.builder()
                                .id("r3")
                                .values(Map.of("c1", "key3", "c4", "valueC3"))
                                .build(),
                        "3", ConfigTable.Row.builder()
                                .id("r4")
                                .values(Map.of("c1", "key4"))
                                .build()
                ))
                .build();
        var rows = ConfigTableUtils.getRowsByUniqueKeys(t1, "Key Column ", Set.of("key1", "key3"));
        assertThat(rows.values().stream().map(ConfigTable.Row::getId)).containsExactlyInAnyOrder("r2", "r3");
    }
}