package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DevProductivityUserIds.DevProductivityUserIdsBuilder.class)
public class DevProductivityUserIds {
    @JsonProperty("user_id_type")
    private IdType userIdType;

    @JsonProperty("user_id_list")
    private List<UUID> userIdList;

    @JsonProperty("id_type")
    private IdType idType;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("org_ids")
    private List<UUID> orgIds;

    public static DevProductivityUserIds fromListRequest(DefaultListRequest filter) {
        IdType userIdType = IdType.fromString(filter.getFilterValue("user_id_type", String.class).orElse(""));
        List<UUID> userIds = CollectionUtils.emptyIfNull(getListOrDefault(filter, "user_id_list")).stream().map(UUID::fromString).filter(Objects::nonNull).collect(Collectors.toList());

        return DevProductivityUserIds.builder()
                .userIdType(userIdType)
                .userIdList(userIds)
                .build();
    }

}
