package io.levelops.cicd.services;

import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.CICDJobRunCommits;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CiCdService {
    CICDJob getCiCdJob(final String company, final String jobId) throws Exception;
    CICDJobRun getCiCdJobRun(final String company, final String jobRunId) throws Exception;
    JobRunStage getJobRunStage(String company, String stageId) throws Exception;

    DbListResponse<JobRunStage> listJobRunStages(String company, String jobRunId, DefaultListRequest defaultListRequest) throws InternalApiClientException;

    Set<PathSegment> getJobRunFullPath(String company, String jobRunId, boolean bottomUp) throws Exception;

    Set<PathSegment> getJobStageFullPath(String company, String stageId, boolean bottomUp) throws Exception;
}