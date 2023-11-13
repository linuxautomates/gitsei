package io.levelops.integrations.helixcore.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean describing a HelixCoreChangeList from https://www.perforce.com/manuals/p4java-javadoc/com/perforce/p4java/core/IChangelistSummary.html
 * with relevant fields
 */
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

    @JsonProperty
    ChangelistStatus status;

    @JsonProperty
    Date lastUpdatedAt;

    @JsonProperty
    List<HelixCoreFile> files;

    public static HelixCoreChangeList fromIChangeList(IChangelist iChangelist) throws ConnectionException,
            AccessException, RequestException {
        int filesCount = iChangelist.getFiles(true).size();
        return HelixCoreChangeList.builder()
                .author(iChangelist.getUsername())
                .id(iChangelist.getId())
                .description(iChangelist.getDescription())
                .status(ChangelistStatus.fromString(iChangelist.getStatus().toString()))
                .lastUpdatedAt(iChangelist.getDate())
                .filesCount(filesCount)
                .files(iChangelist.getFiles(true).stream().map(HelixCoreFile::fromIFileSpec)
                        .collect(Collectors.toList()))
                .build();
    }

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
