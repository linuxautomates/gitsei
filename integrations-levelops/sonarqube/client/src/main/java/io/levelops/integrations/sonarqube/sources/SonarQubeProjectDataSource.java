package io.levelops.integrations.sonarqube.sources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.Analyse;
import io.levelops.integrations.sonarqube.models.Branch;
import io.levelops.integrations.sonarqube.models.Measure;
import io.levelops.integrations.sonarqube.models.Metric;
import io.levelops.integrations.sonarqube.models.MetricResponse;
import io.levelops.integrations.sonarqube.models.Project;
import io.levelops.integrations.sonarqube.models.ProjectResponse;
import io.levelops.integrations.sonarqube.models.PullRequest;
import io.levelops.integrations.sonarqube.services.SonarQubeProjectEnrichmentService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sonarqube's implementation of the {@link DataSource}. This class can be used to fetch project data enriched with
 * project analyses,project branches,pull requests and issues from Sonarqube.
 */
@Log4j2
public class SonarQubeProjectDataSource implements DataSource<Project, SonarQubeProjectDataSource.SonarQubeProjectQuery> {

    private final SonarQubeClientFactory sonarQubeClientFactory;
    private final SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService;
    private final EnumSet<Enrichment> enrichments;

    public SonarQubeProjectDataSource(SonarQubeClientFactory sonarQubeClientFactory,
                                      SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService) {
        this(sonarQubeClientFactory, EnumSet.noneOf(Enrichment.class), sonarQubeProjectEnrichmentService);
    }

    public SonarQubeProjectDataSource(SonarQubeClientFactory sonarQubeClientFactory, EnumSet<Enrichment> enrichments,
                                      SonarQubeProjectEnrichmentService sonarQubeProjectEnrichmentService) {
        this.enrichments = enrichments;
        this.sonarQubeClientFactory = sonarQubeClientFactory;
        this.sonarQubeProjectEnrichmentService = sonarQubeProjectEnrichmentService;
    }

    @Override
    public Data<Project> fetchOne(SonarQubeProjectQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<Project>> fetchMany(SonarQubeProjectQuery query) throws FetchException {
        SonarQubeClient sonarQubeClient = sonarQubeClientFactory.get(query.getIntegrationKey());
        int startingPage = 1;
        final Map<String, Metric> metrics = PaginationUtils.stream(startingPage, 1, offset -> {
            try {
                final MetricResponse response = sonarQubeClient.getMetrics(offset);
                return response.getMetrics();
            } catch (SonarQubeClientException e) {
                log.error("Encountered SonarQube client error while fetching metrics for integration key: "
                        + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                return List.of();
            }
        }).collect(Collectors.toMap(Metric::getKey, Function.identity()));

        return PaginationUtils.stream(startingPage, 1,
                offset -> getProjects(sonarQubeClient, query, metrics, offset))
                .filter(p -> isProjectSelected(p, query.getProjectKeys()))
                .map(project -> parseAndEnrichProject(sonarQubeClient, query, project, metrics))
                .map(BasicData.mapper(Project.class));
    }

    private List<Project> getProjects(SonarQubeClient sonarQubeClient,
                                            SonarQubeProjectQuery query,
                                            Map<String, Metric> metrics,
                                            int offset) {
        try {
            ProjectResponse projectResponse = sonarQubeClient.getProjects(query.getUsePrivilegedAPIs(), offset);
            log.info("ProjectResponse are : {} ",projectResponse);
            if(projectResponse == null)
                return List.of();
            return projectResponse.getProjects();
        } catch (SonarQubeClientException e) {
            log.warn("Failed to get issues after page {}", offset, e);
            throw new RuntimeStreamException("Failed to get issues after page=" + offset, e);
        }
    }

    public static boolean isProjectSelected(Project project, @Nullable Set<String> projectKeys) {
        if (CollectionUtils.isEmpty(projectKeys)) {
            return true; // if no filter, scan all projects
        }
        if (project == null) {
            return false;
        }
        return projectKeys.contains(StringUtils.defaultString(project.getKey())); // case sensitive
    }

    private Project parseAndEnrichProject(SonarQubeClient sonarQubeClient,
                                          SonarQubeProjectQuery query,
                                          Project project, Map<String, Metric> metrics) {
        List<Measure> measures = null;
        List<Analyse> analyses = null;
        List<Branch> branches = null;
        List<PullRequest> pullRequests = null;
        try {
            if (enrichments.contains(Enrichment.MEASURE)) {
                measures = sonarQubeProjectEnrichmentService.getMeasures(sonarQubeClient, project.getKey(), metrics);
            }
            if (enrichments.contains(Enrichment.PROJECT_ANALYSIS)) {
                analyses = sonarQubeProjectEnrichmentService
                        .enrichProjectAnalyses(sonarQubeClient, query, project);
            }
            if (enrichments.contains(Enrichment.PROJECT_BRANCH)) {
                branches = sonarQubeProjectEnrichmentService
                        .enrichProjectBranches(sonarQubeClient, query, project, metrics);
            }
            if (enrichments.contains(Enrichment.PULL_REQUEST_ISSUE)) {
                pullRequests = sonarQubeProjectEnrichmentService
                        .enrichPullRequest(sonarQubeClient, query, metrics, project);
            }
        } catch (SonarQubeClientException e) {
            log.error("Encountered SonarQube client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered SonarQube client error for integration key: " +
                    query.getIntegrationKey(), e);
        }
        return project.toBuilder()
                .measures(measures)
                .analyses(analyses)
                .branches(branches)
                .pullRequests(pullRequests)
                .build();
    }

    public enum Enrichment {
        PROJECT_ANALYSIS, PROJECT_BRANCH, PULL_REQUEST_ISSUE, MEASURE
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SonarQubeProjectDataSource.SonarQubeProjectQuery.SonarQubeProjectQueryBuilder.class)
    public static class SonarQubeProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("from")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
        Date from;

        @JsonProperty("to")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
        Date to;

        /**
         * To restrict which projects will be ingested.
         */
        @JsonProperty("project_keys")
        Set<String> projectKeys;

        @JsonProperty("use_privileged_APIs")
        Boolean usePrivilegedAPIs;
    }
}