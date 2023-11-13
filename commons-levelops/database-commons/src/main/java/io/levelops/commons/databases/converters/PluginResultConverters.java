package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.DbPluginResultLabel;
import io.levelops.commons.dates.DateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PluginResultConverters {

    public static DbPluginResult convertFromDTO(PluginResultDTO dto) {
        return DbPluginResult.builder()
                .tool(dto.getTool())
                .version(dto.getVersion())
                .productIds(dto.getProductIds().stream().map(Integer::valueOf).collect(Collectors.toList()))
                .successful(dto.getSuccessful())
                .metadata(dto.getMetadata())
                .labels(convertLabelsFromDTO(dto.getLabels()))
                .createdAt(DateUtils.toEpochSecond(dto.getCreatedAt()))
                .build();
    }

    public static PluginResultDTO convertToDTO(DbPluginResult result, Map<String, Object> results) {
        return PluginResultDTO.builder()
                .id(result.getId())
                .tool(result.getTool())
                .pluginClass(result.getPluginClass())
                .pluginName(result.getPluginName())
                .version(result.getVersion())
                .productIds(CollectionUtils.isEmpty(result.getProductIds()) ? List.of(): result.getProductIds().stream().map(String::valueOf).collect(Collectors.toList()))
                .successful(result.getSuccessful())
                .metadata(result.getMetadata())
                .labels(convertLabelsToDTO(result.getLabels()))
                .results(results)
                .createdAt(DateUtils.fromEpochSecondToDate(result.getCreatedAt()))
                .build();
    }


    public static List<DbPluginResultLabel> convertLabelsFromDTO(Map<String, List<String>> labels) {
        if (MapUtils.isEmpty(labels)) {
            return Collections.emptyList();
        }
        return labels.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> Strings.isNotEmpty(entry.getKey()) && CollectionUtils.isNotEmpty(entry.getValue()))
                .flatMap(entry -> entry.getValue().stream()
                        .filter(Strings::isNotEmpty)
                        .map(val -> DbPluginResultLabel.builder()
                                .key(entry.getKey())
                                .value(val)
                                .build()))
                .collect(Collectors.toList());
    }

    public static Map<String, List<String>> convertLabelsToDTO(List<DbPluginResultLabel> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return Collections.emptyMap();
        }
        return labels.stream()
                .filter(Objects::nonNull)
                .filter(entry -> Strings.isNotEmpty(entry.getKey()) && Strings.isNotEmpty(entry.getValue()))
                .collect(Collectors.toMap(
                        DbPluginResultLabel::getKey,
                        label -> {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(label.getValue());
                            return list;
                        },
                        (a, b) -> {
                            a.addAll(b);
                            return a;
                        }));
    }

}
