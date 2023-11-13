package io.levelops.aggregations.helpers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.aggregations.services.DroneCIBuildService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for performing droneci aggregations
 */
@Log4j2
@Service
public class DroneCIAggHelper {
    private static final String REPOS_DATATYPE = "repository";

    private Storage storage;
    private final JobDtoParser jobDtoParser;
    private final DroneCIBuildService droneciBuildService;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;

    @Autowired
    public DroneCIAggHelper(JobDtoParser jobDtoParser,
                            DroneCIBuildService droneciBuildService,
                            CiCdInstancesDatabaseService ciCdInstancesDatabaseService) {
        this.jobDtoParser = jobDtoParser;
        this.droneciBuildService = droneciBuildService;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
    }

    public void setupDroneCIBuilds(String customer, String integrationId,
                                   MultipleTriggerResults results) throws SQLException {
        UUID instanceId = getCiCdInstanceId(customer, integrationId);
        this.droneciBuildService.setStorage(storage);
        jobDtoParser.applyToResults(customer,
                REPOS_DATATYPE,
                DroneCIEnrichRepoData.class,
                results.getTriggerResults().get(0),
                repo -> droneciBuildService.insert(customer, instanceId, repo),
                List.of());
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    private UUID getCiCdInstanceId(String company, String integrationId) throws SQLException {
        DbListResponse<CICDInstance> dbListResponse = ciCdInstancesDatabaseService
                .list(company,
                        CICDInstanceFilter.builder()
                                .integrationIds(List.of(integrationId))
                                .types(List.of(CICD_TYPE.droneci))
                                .build(), null, null, null);
        if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
            return dbListResponse.getRecords().get(0).getId();
        } else {
            log.warn("CiCd instance response is empty for company " + company + "and integration id" + integrationId);
            throw new RuntimeException("Error listing the cicd instances for integration id " + integrationId + " type "
                    + CICD_TYPE.droneci);
        }
    }
}
