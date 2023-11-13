package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Log4j2
public class DbScmReview {
    @JsonProperty("id")
    String id;

    @JsonProperty("pr_id")
    String prId;

    @JsonProperty("review_id")
    String reviewId; //this will be inserted during db write

    @JsonProperty("reviewer")
    String reviewer;

    @JsonProperty("reviewer_id")
    String reviewerId;

    @JsonProperty("reviewer_info")
    DbScmUser reviewerInfo; //contains DbScmUser object for reviewer

    @JsonProperty("state")
    String state;

    @JsonProperty("reviewed_at")
    Long reviewedAt;
}
