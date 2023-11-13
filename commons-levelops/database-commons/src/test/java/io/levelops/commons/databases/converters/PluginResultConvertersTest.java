package io.levelops.commons.databases.converters;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.plugins.DbPluginResultLabel;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginResultConvertersTest {

    @Test
    public void convertLabelsFromDTO() {
        Map<String, List<String>> labels = Maps.newHashMap();
        labels.put("a", Lists.newArrayList("1", null, "", "2"));
        labels.put("b", Lists.newArrayList("3"));
        labels.put("", Lists.newArrayList("nooo"));
        labels.put(null, List.of("value"));
        List<DbPluginResultLabel> output = PluginResultConverters.convertLabelsFromDTO(labels);
        assertThat(output).containsExactlyInAnyOrder(
                DbPluginResultLabel.builder()
                        .key("a")
                        .value("1")
                        .build(),
                DbPluginResultLabel.builder()
                        .key("a")
                        .value("2")
                        .build(),
                DbPluginResultLabel.builder()
                        .key("b")
                        .value("3")
                        .build());
    }

    @Test
    public void convertLabelsToDTO() {

        List<DbPluginResultLabel> labels = List.of(
                DbPluginResultLabel.builder()
                        .key("a")
                        .value("1")
                        .build(),
                DbPluginResultLabel.builder()
                        .key("a")
                        .value("2")
                        .build(),
                DbPluginResultLabel.builder()
                        .key("b")
                        .value("3")
                        .build());
        Map<String, List<String>> output = PluginResultConverters.convertLabelsToDTO(labels);

        assertThat(output).containsExactlyInAnyOrderEntriesOf(Map.of(
                "a", List.of("1", "2"),
                "b", List.of("3")
        ));
    }
}