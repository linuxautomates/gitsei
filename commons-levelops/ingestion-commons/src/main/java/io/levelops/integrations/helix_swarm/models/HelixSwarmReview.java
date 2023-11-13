package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmReview.HelixSwarmReviewBuilder.class)
public class HelixSwarmReview {

    @JsonProperty("id")
    Long id;

    //This is SCM PR creator
    @JsonProperty("author")
    String author;

    //I believe this is not used
    @JsonProperty("changes")
    List<Long> changes;

    //I believe this is not used
    @JsonProperty("comments")
    Object comments;

    @JsonProperty("commits")
    List<Long> commits;

    //I believe this is not used
    @JsonProperty("commitStatus")
    Object commitStatus;

    @JsonProperty("created")
    Long createdAt;

    @JsonProperty("description")
    String description;

    //I believe this is not used
    @JsonProperty("groups")
    List<String> groups;

    //This is SCM PR Assignees
    @JsonProperty("participants")
    JsonNode participants;

    //I believe this is not used
    @JsonProperty("pending")
    Boolean pending;

    //I believe this is not used
    @JsonProperty("projects")
    JsonNode projects;

    @JsonProperty("state")
    String state;

    //I believe this is not used
    @JsonProperty("stateLabel")
    String stateLabel;

    //I believe this is not used
    @JsonProperty("testStatus")
    String testStatus;

    //I believe this is not used
    @JsonProperty("type")
    String type;

    //I believe this is not used
    @JsonProperty("updated")
    Long updated;

    //@JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ssZ")
    @JsonProperty("updateDate")
    String updatedAt;

    @JsonProperty("versions")
    List<HelixSwarmChange> versions; //enriched

    //This is PR Reviews
    @JsonProperty("reviews")
    List<HelixSwarmActivity> reviews; //enriched

    //When we try to fetch review files info using v10, if we get 404, we set reviewFilesApiNotFound to true
    @JsonProperty("review_files_api_not_found")
    Boolean reviewFilesApiNotFound; //enriched

    @JsonProperty("file_infos")
    List<ReviewFileInfo> fileInfos; //enriched

    public static Instant parseReviewUpdateDate(final String dateString) {
        if(StringUtils.isBlank(dateString)) {
            return null;
        }
        try {
            Instant date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(dateString).toInstant();
            return date;
        } catch (ParseException e) {
            log.warn("Error parsing date string \"" + dateString  + "\"");
            return null;
        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }
}
