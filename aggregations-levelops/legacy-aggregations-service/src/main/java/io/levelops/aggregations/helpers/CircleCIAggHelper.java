package io.levelops.aggregations.helpers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations.services.CircleCIBuildService;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Log4j2
@Service
public class CircleCIAggHelper {

    private static final String BUILDS_DATA_TYPE = "builds";

    private Storage storage;
    private final JobDtoParser jobDtoParser;
    private final IntegrationTrackingService trackingService;
    private final CircleCIBuildService circleCIBuildService;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;

    public CircleCIAggHelper(JobDtoParser jobDtoParser,
                             CircleCIBuildService circleCIBuildService,
                             CiCdInstancesDatabaseService ciCdInstancesDatabaseService,
                             IntegrationTrackingService trackingService) {
        this.jobDtoParser = jobDtoParser;
        this.circleCIBuildService = circleCIBuildService;
        this.trackingService = trackingService;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public boolean setupCircleCIBuilds(String customer,
                                       String integrationId,
                                       MultipleTriggerResults triggerResults,
                                       Date currentTime) throws SQLException {
        Optional<TriggerResults> latestTriggerResult = triggerResults.getTriggerResults().stream().findFirst();
        boolean result = false;
        UUID instanceId = getCiCdInstanceId(customer, integrationId);
        this.circleCIBuildService.setStorage(storage);
        if (latestTriggerResult.isPresent()) {
            result = jobDtoParser.applyToResults(customer,
                    BUILDS_DATA_TYPE,
                    CircleCIBuild.class,
                    latestTriggerResult.get(),
                    build -> circleCIBuildService.insert(customer, instanceId, build),
                    List.of());
            if (result) {
                trackingService.upsert(customer,
                        IntegrationTracker.builder()
                                .integrationId(integrationId)
                                .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE))
                                .build());
            }
        }
        return result;
    }
    private UUID getCiCdInstanceId(String company, String integrationId) throws SQLException {
        DbListResponse<CICDInstance> dbListResponse = ciCdInstancesDatabaseService
                .list(company,
                        CICDInstanceFilter.builder()
                                .integrationIds(List.of(integrationId))
                                .types(List.of(CICD_TYPE.circleci))
                                .build(), null, null, null);
        if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
            return dbListResponse.getRecords().get(0).getId();
        } else {
            log.warn("CiCd instance response is empty for company " + company + "and integration id" + integrationId);
            throw new RuntimeException("Error listing the cicd instances for integration id " + integrationId + " type "
                    + CICD_TYPE.circleci);
        }
    }
}
