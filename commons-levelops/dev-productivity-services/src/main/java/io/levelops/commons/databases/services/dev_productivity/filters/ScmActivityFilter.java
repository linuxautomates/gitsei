package io.levelops.commons.databases.services.dev_productivity.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Value
@Builder(toBuilder = true)
public class ScmActivityFilter {
    //User will send DefaultListRequest to Server Api
    //Server Api will convert DefaultListRequest to ScmActivityFilter
    //In Server Api we will user ou_ref_ids to integrationUserIds
    //Call ScmActivitiesEngine with across, timerange & integrationUserIds
    @JsonProperty("across")
    ScmActivityFilter.DISTINCT across;

    @JsonProperty("ou_ref_ids")
    List<Integer> ouRefIds;

    @JsonProperty("time_range")
    ImmutablePair<Long, Long> timeRange;

    @JsonProperty("user_id_type")
    private IdType userIdType;

    @JsonProperty("user_id")
    private UUID userId;

    public enum DISTINCT {
        repo_id,
        integration_user;

        public static ScmActivityFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(ScmActivityFilter.DISTINCT.class, st);
        }
    }

    public static ScmActivityFilter fromListRequest(DefaultListRequest filter) {
        String userIdString = filter.getFilterValue("user_id", String.class).orElse(null);
        UUID userId = (StringUtils.isNotEmpty(userIdString)) ? UUID.fromString(userIdString) : null;
        return ScmActivityFilter.builder()
                .ouRefIds(CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter.getFilter(), "ou_ref_ids")).stream().map(Integer::parseInt).collect(Collectors.toList()))
                .across(DISTINCT.fromString(filter.getFilterValue("across", String.class).orElse(null)))
                .userIdType(IdType.fromString(filter.getFilterValue("user_id_type", String.class).orElse(null)))
                .userId(userId)
                .timeRange(filter.getNumericRangeFilter("time_range"))
                .build();
    }
}
