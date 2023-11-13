package io.levelops.commons.databases.models.database.dev_productivity;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DevProductivityResponse.DevProductivityResponseBuilder.class)
public class DevProductivityResponse {
    @JsonProperty("org_user_id")
    private final UUID orgUserId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;

    @JsonProperty("org_id")
    private final UUID orgId;

    @JsonProperty("org_name")
    private String orgName;

    @JsonProperty("org_ref_id")
    private String orgRefId;

    /*
    Add some more user details
     */
    @JsonProperty("section_responses")
    private List<SectionResponse> sectionResponses;

    @JsonProperty("score")
    private final Integer score;

    //region Report Interval Details
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("start_time")
    private final Long startTime; //Date serializes to ms, we need secs so using Long
    @JsonProperty("end_time")
    private final Long endTime;  //Date serializes to ms, we need secs so using Long
    //endregion

    //region Report Completeness

    //region Report Completeness - User
    @JsonProperty("incomplete")
    private final Boolean incomplete;
    @JsonProperty("missing_features")
    private final List<String> missingFeatures;
    //endregion

    //region Report Completeness - Org
    @JsonProperty("missing_user_reports_count")
    private final Integer missingUserReportsCount;
    @JsonProperty("stale_user_reports_count")
    private final Integer staleUserReportsCount;
    //endregion

    @JsonProperty("result_time")
    private final Long resultTime;  //Date serializes to ms, we need secs so using Long
    //endregion

    //region sub-profile details
    private final Integer order; //index of sub-profile in the parent-profile
    //end region

}
