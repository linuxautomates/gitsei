package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * CI/CD Artifact mapped to a job run.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdJobRunArtifact.CiCdJobRunArtifactBuilder.class)
public class CiCdJobRunArtifact {

    public static final String CONTAINER_TYPE = "container";

    @JsonProperty("id")
    String id;

    @JsonProperty("cicd_job_run_id")
    UUID cicdJobRunId;

    /**
     * True when the job run consumed this artifact. (e.g. deployment)
     * Null if we couldn't determine either way.
     */
    @JsonProperty("input")
    Boolean input;

    /**
     * True when the job run produced this artifact. (e.g. build)
     * Null if we couldn't determine either way.
     */
    @JsonProperty("output")
    Boolean output;

    /**
     * Artifact type e.g. docker, jar, s3 file, etc.
     */
    @JsonProperty("type")
    String type;

    /**
     * Artifact location e.g. url of artifact registry, url of s3 bucket, etc.
     */
    @JsonProperty("location")
    String location;

    /**
     * Artifact name e.g. docker image name, jar name, etc.
     */
    @JsonProperty("name")
    String name;

    /**
     * Artifact qualifier e.g. docker image tag, version, etc.
     */
    @JsonProperty("qualifier")
    String qualifier;

    /**
     * Artifact hash.
     */
    @JsonProperty("hash")
    String hash;

    /**
     * Any additional metadata can be stored here.
     */
    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @JsonProperty("created_at")
    Instant createdAt;

    /**
     * Compare 2 artifacts and return true when the significant data is the same,
     * ignoring fields like database id or created at timestamp.
     */
    public boolean compareTo(CiCdJobRunArtifact other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        return new EqualsBuilder()
                .append(cicdJobRunId, other.cicdJobRunId)
                .append(input, other.input)
                .append(output, other.output)
                .append(type, other.type)
                .append(location, other.location)
                .append(name, other.name)
                .append(qualifier, other.qualifier)
                .append(hash, other.hash)
                .append(metadata, other.metadata)
                .isEquals();
    }

}
