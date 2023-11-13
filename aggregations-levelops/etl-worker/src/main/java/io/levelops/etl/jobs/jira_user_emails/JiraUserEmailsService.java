package io.levelops.etl.jobs.jira_user_emails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.etl.utils.IngestionJobUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.JiraUserEmailQuery;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneJobService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.jira.models.JiraUserEmail;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class JiraUserEmailsService extends BaseIngestionService {
    public static int MAX_CLOUD_IDS_PER_PAGE = 5000;
    private final ControlPlaneJobService controlPlaneJobService;
    private final ObjectMapper objectMapper;
    private final GcsUtils gcsUtils;
    private final UserIdentityService userIdentityService;
    private final ControlPlaneService controlPlaneService;
    private final InventoryService inventoryService;

    public JiraUserEmailsService(
            ControlPlaneJobService controlPlaneJobService,
            ObjectMapper objectMapper,
            GcsUtils gcsUtils,
            UserIdentityService userIdentityService,
            ControlPlaneService controlPlaneService,
            InventoryService inventoryService) {
        super(controlPlaneService, inventoryService, "JiraUserEmailIntegrationController");
        this.controlPlaneJobService = controlPlaneJobService;
        this.objectMapper = objectMapper;
        this.gcsUtils = gcsUtils;
        this.userIdentityService = userIdentityService;
        this.controlPlaneService = controlPlaneService;
        this.inventoryService = inventoryService;
    }

    // Returns list of completed ingestion job ids
    public List<String> processIngestionJobs(List<String> jobIds, String tenantId, String integrationId) throws IngestionServiceException {
        return jobIds.stream()
                .map(jobId -> {
                    try {
                        return controlPlaneJobService.getJobIfComplete(jobId);
                    } catch (IngestionServiceException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jobDTO -> {
                    processIngestionJob(jobDTO, tenantId, integrationId);
                    return jobDTO.getId();
                })
                .collect(Collectors.toList());
    }

    /**
     * Looks at all integration users that do not have a valid email id and returns their cloud ids
     */
    public List<String> getIncrementalPayload(JobContext context) {
        return userIdentityService.stream(
                        context.getTenantId(),
                        UserIdentitiesFilter.builder()
                                .integrationIds(List.of(context.getIntegrationId()))
                                .emptyEmails(true)
                                .build())
                .map(DbScmUser::getCloudId)
                .collect(Collectors.toList());
    }

    /**
     * Looks at all integration users and returns their cloud ids
     */
    public List<String> getFullPayload(JobContext context) {
        return userIdentityService.stream(
                        context.getTenantId(),
                        UserIdentitiesFilter.builder()
                                .integrationIds(List.of(context.getIntegrationId()))
                                .build())
                .map(DbScmUser::getCloudId)
                .collect(Collectors.toList());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }

    /**
     * Takes in a list of cloud ids and creates ingestion jobs to fetch emails from them
     *
     * @return list of ingestion job ids
     */
    public List<String> createIngestionJobs(List<String> cloudIds, JobContext context) throws IngestionServiceException {
        var integrationKey = IntegrationKey.builder()
                .integrationId(context.getIntegrationId())
                .tenantId(context.getTenantId())
                .build();
        return Lists.partition(cloudIds, MAX_CLOUD_IDS_PER_PAGE)
                .stream()
                .map(accountIds -> {
                    try {
                        return submitJob("",
                                integrationKey,
                                JiraUserEmailQuery.builder()
                                        .integrationKey(integrationKey)
                                        .accountIds(accountIds)
                                        .build()
                        );
                    } catch (IngestionServiceException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(SubmitJobResponse::getJobId)
                .collect(Collectors.toList());
    }

    private Stream<JiraUserEmail> getGcsResults(List<GcsDataResultWithDataType> gcsDataResultWithDataTypes) {
        return gcsDataResultWithDataTypes.stream().flatMap(gcsDataResultWithDataType -> {
            try {
                return gcsUtils.fetchRecordsFromGcs(
                        gcsDataResultWithDataType.getGcsDataResult(),
                        JiraUserEmail.class,
                        objectMapper.constructType(JiraUserEmail.class),
                        "",
                        "").getData().getRecords().stream();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Stream<JiraUserEmail> getUserEmailsFromJobResults(JobDTO jobDTO) {
        var gcsResults = IngestionJobUtils.getSinglePageIngestionJobResults(jobDTO, objectMapper);
        return getGcsResults(gcsResults);
    }

    protected void processUserEmail(JiraUserEmail jiraUserEmail, String tenantId, String integrationId) throws SQLException {
        Optional<DbScmUser> userOpt = userIdentityService.getUserByCloudId(tenantId, integrationId, jiraUserEmail.getAccountId());
        if (userOpt.isPresent()) {
            DbScmUser user = userOpt.get();
            if (StringUtils.isNotEmpty(jiraUserEmail.getEmail())) {
                Set<String> updateEmails = new HashSet<>(
                        user.getEmails().stream()
                                .map(String::toLowerCase)
                                .toList());
                updateEmails.add(jiraUserEmail.getEmail().toLowerCase());
                DbScmUser updatedUser = user.toBuilder()
                        .emails(updateEmails.stream().toList())
                        .build();
                userIdentityService.upsert(tenantId, updatedUser);
            } else {
                log.warn("User {} has no email in Jira", user.getCloudId());
            }
        } else {
            log.error("User {} not found in DB. This should never happen", jiraUserEmail.getAccountId());
        }
    }

    private void processIngestionJob(JobDTO jobDTO, String tenantId, String integrationId) {
        if (!jobDTO.getStatus().equals(JobStatus.SUCCESS)) {
            log.warn("Ingestion job for fetching user emails {} failed", jobDTO.getId());
            return;
        }
        Stream<JiraUserEmail> jiraUserEmailStream = getUserEmailsFromJobResults(jobDTO);
        jiraUserEmailStream.forEach(jiraUserEmail -> {
            try {
                processUserEmail(jiraUserEmail, tenantId, integrationId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}