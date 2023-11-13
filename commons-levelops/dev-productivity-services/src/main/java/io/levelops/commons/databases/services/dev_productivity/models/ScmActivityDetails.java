package io.levelops.commons.databases.services.dev_productivity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScmActivityDetails.ScmActivityDetailsBuilder.class)
public class ScmActivityDetails {
    @JsonProperty("key")
    private String dayOfWeek;

    @JsonProperty("num_prs_created")
    private Integer numPrsCreated;
    @JsonProperty("num_prs_merged")
    private Integer numPrsMerged;
    @JsonProperty("num_prs_closed")
    private Integer numPrsClosed;
    @JsonProperty("num_prs_comments")
    private Integer numPrsComments;
    @JsonProperty("num_commits_created")
    private Integer numCommitsCreated;

    public static ScmActivityDetails merge(List<ScmActivityDetails> scmActivityDetailsList) {
        if(CollectionUtils.isEmpty(scmActivityDetailsList)) {
            return ScmActivityDetails.builder().build();
        }
        Integer numPrsCreated = scmActivityDetailsList.stream()
                .filter(x -> x.numPrsCreated != null)
                .mapToInt(ScmActivityDetails::getNumPrsCreated)
                .sum();
        Integer numPrsMerged = scmActivityDetailsList.stream()
                .filter(x -> x.numPrsMerged != null)
                .mapToInt(ScmActivityDetails::getNumPrsMerged)
                .sum();
        Integer numPrsClosed = scmActivityDetailsList.stream()
                .filter(x -> x.numPrsClosed != null)
                .mapToInt(ScmActivityDetails::getNumPrsClosed)
                .sum();
        Integer numPrsComments = scmActivityDetailsList.stream()
                .filter(x -> x.numPrsComments != null)
                .mapToInt(ScmActivityDetails::getNumPrsComments)
                .sum();
        Integer numCommitsCreated = scmActivityDetailsList.stream()
                .filter(x -> x.numCommitsCreated != null)
                .mapToInt(ScmActivityDetails::getNumCommitsCreated)
                .sum();
        return ScmActivityDetails.builder()
                .dayOfWeek(scmActivityDetailsList.get(0).getDayOfWeek())
                .numPrsCreated(numPrsCreated)
                .numPrsMerged(numPrsMerged)
                .numPrsClosed(numPrsClosed)
                .numPrsComments(numPrsComments)
                .numCommitsCreated(numCommitsCreated)
                .build();
    }
}
