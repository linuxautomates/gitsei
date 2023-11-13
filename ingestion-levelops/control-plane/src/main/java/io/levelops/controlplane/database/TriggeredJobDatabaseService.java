package io.levelops.controlplane.database;

import io.levelops.commons.models.DbListResponse;
import io.levelops.controlplane.models.DbIteration;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface TriggeredJobDatabaseService extends DatabaseService<DbTriggeredJob> {

    List<DbIteration> getIterationsByTriggerId(String triggerId, int skip, int limit);

    List<DbTriggeredJob> getTriggeredJobsByIterationId(String iterationId);

    Optional<DbTriggeredJob> getTriggeredJobByJobId(String jobId);

    Optional<DbTriggeredJob> getTriggeredJob(@Nonnull String triggerId,
                                             @Nullable TriggeredJobFilter filter);

    Stream<DbTriggeredJob> streamTriggeredJobs(@Nonnull String triggerId,
                                               @Nullable TriggeredJobFilter filter,
                                               @Nullable Integer lastNIterations);

    DbListResponse<DbTriggeredJob> filterTriggeredJobs(int skip, int limit, @Nonnull String triggerId,
                                                       @Nullable TriggeredJobFilter filter,
                                                       @Nullable Integer lastNIterations);

    @Value
    @Builder(toBuilder = true)
    class TriggeredJobFilter {
        @Nullable
        List<JobStatus> statuses;
        @Nullable
        Boolean partial;
        @Nullable
        Long afterExclusive;
        @Nullable
        Long afterInclusive;
        @Nullable
        Long beforeInclusive;
        @Nullable
        Boolean returnTotalCount;
        @Nullable
        Integer belowMaxAttemptsOrDefaultValue;
    }

    boolean createTriggeredJob(String jobId, String triggerId, String iterationId, Long iterationTs, boolean partial);

    int deleteTriggeredJobs(String triggerId);

}
