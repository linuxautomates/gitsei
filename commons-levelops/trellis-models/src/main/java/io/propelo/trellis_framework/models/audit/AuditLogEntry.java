package io.propelo.trellis_framework.models.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.propelo.trellis_framework.models.jobs.DevProductivityJobParam;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Log4j2
@Builder(toBuilder = true)
@Value
@JsonDeserialize(builder= AuditLogEntry.AuditLogEntryBuilder.class)
public class AuditLogEntry {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("tenant_id")
    private final String tenantId;
    @JsonProperty("source")
    private final AuditLogSourceType source;
    @JsonProperty("job_type")
    private final String jobType;
    @JsonProperty("status")
    private final JobStatus status;
    @JsonProperty("duration_in_secs")
    private final Long durationInSecs;
    @JsonProperty("ref_id")
    private final Integer refId;
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("dev_productivity_profile_id")
    private final UUID devProductivityProfileId;
    @JsonProperty("event_ids")
    private final List<UUID> eventIds;

    @JsonProperty("full_recompute")
    private final Boolean fullRecompute;

    @JsonProperty("job_params")
    private final List<DevProductivityJobParam> jobParams;
    @JsonProperty("output_params")
    private final List<DevProductivityJobParam> outputParams;

    @JsonProperty("created_at")
    private final Instant createdAt;
}