package io.levelops.commons.faceted_search.db.models.workitems;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsDevProdWorkItemResponse.EsDevProdWorkItemResponseBuilder.class)
public class EsDevProdWorkItemResponse {

    @JsonProperty("workitem_id")
    String workitemId;

    @JsonProperty("story_points")
    Double storyPoints;

    @JsonProperty("historical_assignee_id")
    String historicalAssigneeId;

    @JsonProperty("time_in_statuses")
    Double timeInStatuses;

    @JsonProperty("assignee_time")
    Double assigneeTime;

    @JsonProperty("story_points_portion")
    Double storyPointsPortion;

    @JsonProperty("ticket_portion")
    Double ticketPortion;

}
