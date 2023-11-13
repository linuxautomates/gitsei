package io.levelops.ingestion.models.controlplane;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IngestionFailure;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IngestionLogDTO.IngestionLogDTOBuilder.class)
public class IngestionLogDTO {
    @JsonProperty("id")
    String id;
    @JsonProperty("status")
    String status;
    @JsonProperty("description")
    String description;
    @JsonProperty("attempt_count")
    Integer attemptCount;
    @JsonProperty("from")
    Long from;
    @JsonProperty("to")
    Long to;
    @JsonProperty("created_at")
    Long createdAt;
    @JsonProperty("updated_at")
    Long updatedAt;
    @JsonProperty("elapsed")
    Long elapsed;
    @JsonProperty("error")
    Map<String, Object> error; // critical error
    @JsonProperty("failures")
    List<IngestionFailure> ingestionFailures;
    @JsonProperty("is_empty_result")
    Boolean isEmptyResult;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm", Locale.US);

    public static IngestionLogDTO fromJobDTO(ObjectMapper objectMapper, JobDTO jobDTO) {
        ImmutablePair<Long, Long> fromAndTo = extractFromAndTo(objectMapper, jobDTO);
        Long from = fromAndTo.getLeft();
        Long to = fromAndTo.getRight();
        Boolean isEmptyResult = null;
        if (jobDTO.getResult() != null) {
            isEmptyResult = jobDTO.getResult().containsKey("empty");
        }
        return IngestionLogDTO.builder()
                .id(jobDTO.getId())
                .createdAt(jobDTO.getCreatedAt())
                .updatedAt(jobDTO.getStatusChangedAt())
                .status(convertJobStatusToDTO(jobDTO.getStatus()))
                .description(generateDescription(from, to))
                .attemptCount(jobDTO.getAttemptCount())
                .from(from)
                .to(to)
                .elapsed(getElapsedTimeSeconds(jobDTO))
                .error(jobDTO.getError())
                .ingestionFailures(jobDTO.getIngestionFailures())
                .isEmptyResult(isEmptyResult)
                .build();
    }

    public static String generateDescription(Long from, Long to) {
        String fromStr = "-";
        if (from != null) {
            fromStr = FORMATTER.format(Instant.ofEpochSecond(from).atZone(ZoneOffset.UTC));
        }
        String toStr = "-";
        if (to != null) {
            toStr = FORMATTER.format(Instant.ofEpochSecond(to).atZone(ZoneOffset.UTC));
        }
        return String.format("Scanning activity from %s to %s", fromStr, toStr);
    }

    public static String convertJobStatusToDTO(JobStatus jobStatus) {
        switch (jobStatus) {
            case UNASSIGNED:
            case SCHEDULED:
                return "scheduled";
            case ACCEPTED:
            case PENDING:
                return "pending";
            case SUCCESS:
                return "success";
            case FAILURE:
            case INVALID:
            case CANCELED:
            case ABORTED:
            default:
                return "failure";
        }
    }

    @Nullable
    public static List<JobStatus> convertDTOStatusToJobStatuses(String dtoStatus) {
        switch (StringUtils.trimToEmpty(dtoStatus).toLowerCase()) {
            case "scheduled":
                return List.of(JobStatus.UNASSIGNED, JobStatus.SCHEDULED);
            case "pending":
                return List.of(JobStatus.ACCEPTED, JobStatus.PENDING);
            case "success":
                return List.of(JobStatus.SUCCESS);
            case "failure":
                return List.of(JobStatus.FAILURE, JobStatus.INVALID, JobStatus.CANCELED, JobStatus.ABORTED);
            default:
                return null;
        }
    }

    // returns epoch seconds
    public static ImmutablePair<Long, Long> extractFromAndTo(ObjectMapper objectMapper, JobDTO jobDTO) {
        Map<String, Object> query = objectMapper.convertValue(jobDTO.getQuery(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        Object fromObj = query.get("from");
        Object toObj = query.get("to");
        Long from = null;
        if (fromObj instanceof Long) {
            from = (Long) fromObj;
        }
        Long to = null;
        if (toObj instanceof Long) {
            to = (Long) toObj;
        }
        return ImmutablePair.of(
                from != null ? from / 1000 : null,
                to != null ? to / 1000 : null);
    }

    public static Long getElapsedTimeSeconds(JobDTO jobDTO) {
        Instant createdAt = DateUtils.fromEpochSecond(jobDTO.getCreatedAt());
        Instant updatedAt = DateUtils.fromEpochSecond(jobDTO.getStatusChangedAt());
        if (createdAt == null) {
            return 0L;
        }
        return Math.abs(Duration.between(createdAt, (updatedAt != null)? updatedAt : Instant.now())
                .toSeconds());
    }
}
