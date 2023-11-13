package io.levelops.api.services;

import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CiCdJobsService {
    private static final Boolean STAGES_ON_ALL_LEVELS = false;
    private final CiCdJobRunStageDatabaseService stagesService;
    private final CiCdJobRunsDatabaseService jobRunsService;

    @Autowired
    public CiCdJobsService(CiCdJobRunStageDatabaseService stagesService, final CiCdJobRunsDatabaseService jobRunsDatabaseService) {
        this.stagesService = stagesService;
        this.jobRunsService = jobRunsDatabaseService;
    }

    public DbListResponse<CICDJob> list(String company, int page, int pageSize) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSteps(String company, DefaultListRequest request, int page, int pageSize) {
        // get filters
        var filters = QueryFilter.fromRequestFilters(request.getFilter());
        var strictMatches = new HashMap<>(filters.getStrictMatches());
        if (Strings.isBlank((String) strictMatches.get("job_run_id")) && CollectionUtils.isEmpty((Collection<String>) strictMatches.get("job_ids"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neither the pipeline job_run_id nor the job_id were present but either one of them is required");
        } else if (Strings.isNotBlank((String) strictMatches.get("job_run_id"))) {
            strictMatches.put("cicd_job_run_id", UUID.fromString((String) strictMatches.get("job_run_id")));
        }
        if (CollectionUtils.isNotEmpty((Collection<String>) strictMatches.get("job_ids"))) {
            strictMatches.put("job_ids", strictMatches.get("job_ids"));
        }

        var startTime = request.<String, Object>getFilterValueAsMap("start_time").orElse(Map.of());
        if (MapUtils.isNotEmpty(startTime)) {
            strictMatches.put("start_time", startTime.get("$gt"));
            strictMatches.put("end_time", startTime.get("$lt"));
        } else {
            strictMatches.put("start_time", null);
        }

        // get results
        DbListResponse<JobRunStage> stages;
        try {
            stages = stagesService.list(
                    company,
                    request.getPage(),
                    request.getPageSize(),
                    filters.toBuilder().strictMatches(strictMatches).build(),
                    Pair.of(Set.of("start_time"), SortingOrder.DESC),
                    STAGES_ON_ALL_LEVELS
            );
            if(stages.getTotalCount() > 0) {
                var meta = Map.of(
                    "page", request.getPage(),
                    "page_size", request.getPageSize(),
                    "next_page", stages.getCount() < stages.getTotalCount() ? request.getPage() + 1 : request.getPage(),
                    "has_next", stages.getCount() < stages.getTotalCount(),
                    "total_count", stages.getTotalCount()
                );
                return Map.of(
                            "_metadata", meta,
                            "stages_count", stages.getTotalCount(),
                            "subjobs_count", 0,
                            "count", stages.getCount(),
                            "records", stages.getRecords()
                            );
            }
            var results = jobRunsService.getSubjobs(company, (String) strictMatches.get("job_run_id"), filters, null, page, pageSize);
            return Map.of("_metadata", Map.of(
                        "page", page,
                        "page_size", pageSize,
                        "next_page", results.getCount() < results.getTotalCount() ? page + 1 : page,
                        "has_next", results.getCount() < results.getTotalCount(),
                        "total_count", results.getTotalCount()),
                    "stages_count", 0,
                    "subjobs_count", results.getTotalCount(),
                    "count", results.getCount(),
                    "records", results.getRecords()
            );
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the reques, please try again in a few minutes or contact support. requested id=");
        }
    }
}
