package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.api.converters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DevProductivityFixedIntervalFilter.DevProductivityFixedIntervalFilterBuilder.class)
public class DevProductivityFixedIntervalFilter {
    @JsonProperty("interval")
    ReportIntervalType reportInterval;

    @JsonProperty("ou_ids")
    List<UUID> ouIds;

    @JsonProperty("ou_ref_ids")
    List<Integer> ouRefIds;

    @JsonProperty("sort")
    Map<String, SortingOrder> sort;

    public static DevProductivityFixedIntervalFilter fromListRequest(DefaultListRequest filter) {
        return DevProductivityFixedIntervalFilter.builder()
                .reportInterval(ReportIntervalType.fromString(filter.getFilterValue("interval", String.class).orElse(ReportIntervalType.LAST_QUARTER.toString())))
                .ouIds(CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter.getFilter(), "ou_ids")).stream().map(UUID::fromString).collect(Collectors.toList()))
                .ouRefIds(CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter.getFilter(), "ou_ref_ids")).stream().map(Integer::parseInt).collect(Collectors.toList()))
                .sort(SortingConverter.fromFilter(filter.getSort()))
                .build();
    }
}
