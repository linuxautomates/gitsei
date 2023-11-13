package io.levelops.internal_api.services;

import com.google.common.base.Function;
import io.levelops.commons.databases.models.config_tables.ConfigTable;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTableServiceTest {

    @Test
    void testAddCurrentVersionToHistory() {
        assertThat(ConfigTableService.addCurrentVersionToHistoryAndSort(null)).isNull();
        assertThat(ConfigTableService.addCurrentVersionToHistoryAndSort(ConfigTable.builder().build())).isEqualTo(ConfigTable.builder().build());

        // missing version
        ConfigTable table = ConfigTable.builder()
                .history(Map.of("1", ConfigTable.Revision.builder().version("0").userId("a").createdAt(Instant.MIN).build()))
                .build();
        assertThat(ConfigTableService.addCurrentVersionToHistoryAndSort(table)).isEqualTo(table);

        // test
        table = ConfigTable.builder()
                .version("2")
                .createdBy("me")
                .updatedAt(Instant.MAX)
                .history(Map.of(
                        "1", ConfigTable.Revision.builder().version("1").userId("b").createdAt(Instant.MIN).build(),
                        "0", ConfigTable.Revision.builder().version("0").userId("a").createdAt(Instant.MIN).build()
                ))
                .build();
        ConfigTable out = ConfigTableService.addCurrentVersionToHistoryAndSort(table);

        LinkedHashMap<String, ConfigTable.Revision> expected = new LinkedHashMap<>();
        expected.put("2", ConfigTable.Revision.builder().version("2").userId("me").createdAt(Instant.MAX).build());
        expected.put("1", ConfigTable.Revision.builder().version("1").userId("b").createdAt(Instant.MIN).build());
        expected.put("0", ConfigTable.Revision.builder().version("0").userId("a").createdAt(Instant.MIN).build());
        assertThat(out).isEqualTo(table.toBuilder()
                .history(expected)
                .build());

        // check sort
        assertThat(out.getHistory().keySet()).containsExactly("2", "1", "0");
    }

    @Test
    void testDeleteOldRevisions() {
        final Function<String, ConfigTable.Revision> r = (String v) -> ConfigTable.Revision.builder().version(v).build();
        assertThat(ConfigTableService.findVersionsToRemove(ConfigTable.builder()
                .history(Map.of(
                        "1", r.apply("1"),
                        "2", r.apply("2"),
                        "3", r.apply("3"),
                        "4", r.apply("4"),
                        "5", r.apply("5")))
                .build(), 3))
                .containsExactlyInAnyOrder("1", "2");

        assertThat(ConfigTableService.findVersionsToRemove(ConfigTable.builder()
                .history(Map.of(
                        "1", r.apply("1"),
                        "2", r.apply("2")))
                .build(), 3))
                .isEmpty();

        assertThat(ConfigTableService.findVersionsToRemove(ConfigTable.builder()
                .history(Map.of(
                        "1", r.apply("1"),
                        "2", r.apply("2"),
                        "3", r.apply("3")))
                .build(), 3))
                .isEmpty();

        assertThat(ConfigTableService.findVersionsToRemove(ConfigTable.builder()
                .history(null)
                .build(), 3))
                .isEmpty();
    }
}