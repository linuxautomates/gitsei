package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.plugins.DbPluginLabelsAgg;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.DbPluginResultLabel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class DbPluginResultConverters {

    public static Map<String, Object> parseMetadata(ObjectMapper mapper, @Nullable String metadataJson) throws IOException {
        if (Strings.isEmpty(metadataJson)) {
            return Collections.emptyMap();
        }
        return mapper.readValue(metadataJson,
                mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    public static List<DbPluginResultLabel> parseLabels(ObjectMapper mapper, @Nullable String labelsAgg) throws IOException {
        if (Strings.isEmpty(labelsAgg)) {
            return Collections.emptyList();
        }

        List<DbPluginLabelsAgg> aggs = mapper.readValue(labelsAgg,
                mapper.getTypeFactory().constructCollectionLikeType(List.class, DbPluginLabelsAgg.class));
        if (CollectionUtils.isEmpty(aggs)) {
            return Collections.emptyList();
        }

        List<DbPluginResultLabel> labels = Lists.newArrayList();
        aggs.forEach(agg -> {
            if (Strings.isEmpty(agg.getKey()) || CollectionUtils.isEmpty(agg.getValues())) {
                return;
            }
            agg.getValues().stream()
                    .filter(Objects::nonNull)
                    .map(val -> DbPluginResultLabel.builder()
                            .key(agg.getKey())
                            .value(val)
                            .build())
                    .collect(Collectors.toCollection(() -> labels));
        });
        return labels;
    }

    public static RowMapper<DbPluginResult> rowMapperForList(ObjectMapper mapper) {
        return (rs, rowNum) -> {
            try {
                List<Integer> productIds =rs.getArray("product_ids") != null ? Arrays.asList((Integer[])rs.getArray("product_ids").getArray()) : Collections.emptyList();
                return DbPluginResult.builder()
                        .id(rs.getString("id"))
                        .tool(rs.getString("tool"))
                        .pluginName(rs.getString("name"))
                        .pluginClass(rs.getString("class"))
                        .version(rs.getString("version"))
                        .productIds(productIds)
                        .successful(rs.getBoolean("successful"))
                        .metadata(parseMetadata(mapper, rs.getString("metadata")))
                        .gcsPath(rs.getString("gcs_path"))
                        .createdAt(rs.getLong("created_at"))
                        .labels(parseLabels(mapper, rs.getString("labels_agg")))
                        .build();
            } catch (IOException e) {
                throw new SQLException("Failed to deserialize plugin result with id=" + rs.getString("id"), e);
            }
        };
    }

    public static RowMapper<DbPluginResult> rowMapperForOne(ObjectMapper mapper) {
        return (rs, rowNum) ->
        {
            try {
                return DbPluginResult.builder()
                        .id(rs.getString("id"))
                        .tool(rs.getString("tool"))
                        .pluginName(rs.getString("name"))
                        .pluginClass(rs.getString("class"))
                        .version(rs.getString("version"))
                        .productIds(Arrays.asList((Integer[])rs.getArray("product_ids").getArray()))
                        .successful(rs.getBoolean("successful"))
                        .metadata(parseMetadata(mapper, rs.getString("metadata")))
                        .gcsPath(rs.getString("gcs_path"))
                        .createdAt(rs.getLong("created_at"))
                        .build();
            } catch (IOException e) {
                throw new SQLException("Failed to deserialize plugin result with id=" + rs.getString("id"), e);
            }
        };
    }

    public static RowMapper<DbPluginResultLabel> labelRowMapper() {
        return (rs, rowNum) -> DbPluginResultLabel.builder()
                .key(rs.getString("key"))
                .value(rs.getString("value"))
                .build();
    }

}
