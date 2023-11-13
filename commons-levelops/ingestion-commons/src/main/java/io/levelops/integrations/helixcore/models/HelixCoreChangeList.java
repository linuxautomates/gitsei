package io.levelops.integrations.helixcore.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * Bean describing a HelixCoreChangeList from https://www.perforce.com/manuals/p4java-javadoc/com/perforce/p4java/core/IChangelistSummary.html
 * with relevant fields
 */
@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixCoreChangeList.HelixCoreChangeListBuilder.class)
public class HelixCoreChangeList {

    @JsonProperty
    int id;

    @JsonProperty
    String author;

    @JsonProperty("depot_name")
    String depotName;

    @JsonProperty("stream_name")
    String streamName;

    @JsonProperty
    String description;

    @JsonProperty("files_count")
    int filesCount;

    @JsonProperty("additions")
    int additions;

    @JsonProperty("is_integration_commit")
    boolean isIntegrationCommit;

    @JsonProperty("deletions")
    int deletions;
    
    @JsonProperty("changes")
    int changes;

    @JsonProperty("differences")
    String differences;

    @JsonProperty("diff_parse_error")
    String parseError;

    @JsonProperty
    ChangelistStatus status;

    @JsonProperty
    Date lastUpdatedAt;

    @JsonProperty
    List<HelixCoreFile> files;
    

    /**
     * Describes possible changelist status values.
     */
    public enum ChangelistStatus {
        NEW,
        PENDING,
        SUBMITTED;

        @JsonCreator
        @Nullable
        public static ChangelistStatus fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(ChangelistStatus.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
