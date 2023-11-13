package io.levelops.commons.databases.services.dev_productivity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScmActivities.ScmActivitiesBuilder.class)
public class ScmActivities {
    @JsonProperty("integration_user_id")
    private String integrationUserId;
    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("repo_id")
    private String repoId;

    @JsonProperty("org_user_id")
    private final UUID orgUserId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("stacks")
    List<ScmActivityDetails> activityDetails;

    public static ScmActivities mergeScmActivities (final List<ScmActivities> scmActivitiesList) {
        Map<String, List<ScmActivityDetails>> resultsMap = new HashMap<>();
        for(ScmActivities scmActivities : CollectionUtils.emptyIfNull(scmActivitiesList)) {
            if(CollectionUtils.isEmpty(scmActivities.getActivityDetails())) {
                continue;
            }
            for(ScmActivityDetails scmActivityDetails : scmActivities.getActivityDetails()) {
                resultsMap.putIfAbsent(scmActivityDetails.getDayOfWeek(), new ArrayList<>());
                resultsMap.get(scmActivityDetails.getDayOfWeek()).add(scmActivityDetails);
            }
        }
        List<ScmActivityDetails> scmActivityDetailsList = new ArrayList<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            String key = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault());
            if(resultsMap.get(key) == null) {
                //Create empty ScmActivityDetails
                ScmActivityDetails emptyScmActivityDetails = ScmActivityDetails.builder().dayOfWeek(key).build();
                scmActivityDetailsList.add(emptyScmActivityDetails);
            } else {
                //Merge ScmActivityDetails
                ScmActivityDetails mergedScmActivityDetails = ScmActivityDetails.merge(resultsMap.get(key));
                scmActivityDetailsList.add(mergedScmActivityDetails);
            }
        }
        
        ScmActivities first = (CollectionUtils.isNotEmpty(scmActivitiesList)) ? scmActivitiesList.get(0) : null;
        ScmActivities mergedSCMActivities = ScmActivities.builder()
                .integrationUserId((first == null) ? null: first.getIntegrationUserId())
                .userName((first == null) ? null : first.getUserName())
                .repoId((first == null) ? null : first.getRepoId())
                .activityDetails(scmActivityDetailsList)
                .build();
        return mergedSCMActivities;
    }
}
