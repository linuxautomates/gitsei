package io.levelops.integrations.gerrit.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.client.GerritClientException;
import io.levelops.integrations.gerrit.client.GerritClientFactory;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.levelops.integrations.gerrit.models.ReviewerInfo;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import io.levelops.integrations.gerrit.services.GerritFetchChangesService;
import io.levelops.integrations.gerrit.services.GerritFetchProjectsService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GerritRepositoryDataSource implements DataSource<ProjectInfo, GerritRepositoryDataSource.GerritRepositoryQuery> {

    private static final int ONBOARDING_IN_DAYS = 30;
    private final GerritClientFactory clientFactory;
    private final GerritFetchProjectsService fetchProjectsService;
    private final GerritFetchChangesService fetchChangesService;
    private final EnumSet<Enrichment> enrichments;

    public enum Enrichment {
        PULL_REQUESTS, BRANCHES, TAGS, LABELS, REVIEWERS
    }

    public GerritRepositoryDataSource(GerritClientFactory clientFactory) {
        this(clientFactory, EnumSet.of(Enrichment.BRANCHES, Enrichment.LABELS, Enrichment.TAGS));
    }

    public GerritRepositoryDataSource(GerritClientFactory clientFactory, EnumSet<Enrichment> enrichments) {
        this.clientFactory = clientFactory;
        this.enrichments = enrichments;
        this.fetchProjectsService = new GerritFetchProjectsService();
        this.fetchChangesService = new GerritFetchChangesService();
    }

    private GerritClient getClient(IntegrationKey integrationKey) throws FetchException {
        GerritClient client = null;
        try {
            client = clientFactory.get(integrationKey);
        } catch (GerritClientException e) {
            throw new FetchException("Could not fetch Gerrit client", e);
        }
        return client;
    }

    @Override
    public Data<ProjectInfo> fetchOne(GerritRepositoryQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<ProjectInfo>> fetchMany(GerritRepositoryQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();

        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        GerritClient client = getClient(integrationKey);

        if (enrichments.contains(Enrichment.PULL_REQUESTS) || enrichments.contains(Enrichment.REVIEWERS)) {
            List<ChangeInfo> changes = fetchChangesService.fetchChanges(client, from).collect(Collectors.toList());
            if (enrichments.contains(Enrichment.REVIEWERS)) {
                changes = changes.stream().map(c -> parseAndEnrichChanges(client, c)).collect(Collectors.toList());
            }
            Map<String, List<ChangeInfo>> changeInfoMap = changes.stream().collect(Collectors.groupingBy(ChangeInfo::getProject));
            return changeInfoMap.entrySet().stream().map(changeInfos -> ProjectInfo.builder()
                    .name(changeInfos.getKey())
                    .id(changeInfos.getKey())
                    .changes(changeInfos.getValue()).build())
                    .map(BasicData.mapper(ProjectInfo.class));
        } else {
            return fetchProjectsService.fetchRepos(client)
                    .filter(projectInfo -> {
                        if (CollectionUtils.isNotEmpty(query.getRepos())) {
                            return query.getRepos().contains(projectInfo.getName());
                        } else {
                            return true;
                        }
                    })
                    .map(projectInfo -> parseAndEnrichProjects(client, projectInfo))
                    .map(BasicData.mapper(ProjectInfo.class));
        }
    }

    private ProjectInfo parseAndEnrichProjects(GerritClient client,
                                               ProjectInfo project) {
        List<ProjectInfo.BranchInfo> branches = null;
        List<ProjectInfo.LabelDefinitionInfo> labels = null;
        List<ProjectInfo.TagInfo> tags = null;
        if (enrichments.contains(Enrichment.BRANCHES)) {
            try {
                branches = client.getBranches(project.getId());
            } catch (GerritClientException e) {
                log.warn("failed fetch branches for project {}", project.getName(), e);
            }
        }
        if (enrichments.contains(Enrichment.LABELS)) {
            try {
                labels = client.getLabels(project.getId());
            } catch (GerritClientException e) {
                log.warn("failed fetch labels for project {}", project.getName(), e);
            }
        }
        if (enrichments.contains(Enrichment.TAGS)) {
            try {
                tags = client.getTags(project.getId());
            } catch (GerritClientException e) {
                log.warn("failed fetch tags for project {}", project.getName(), e);
            }
        }
        return project.toBuilder()
                .enrichedLabels(labels)
                .tags(tags)
                .enrichedBranches(branches)
                .build();
    }

    private ChangeInfo parseAndEnrichChanges(GerritClient client, ChangeInfo change) {
        try {
            String changeId = change.getId();
            var revisions = change.getRevisions();
            String currentRevisionId = change.getCurrentRevision();
            if (enrichments.contains(Enrichment.REVIEWERS)) {
                RevisionInfo currentRevision = revisions.get(currentRevisionId);
                List<ReviewerInfo> reviewers = client.getRevisionReviewers(changeId, currentRevisionId);
                currentRevision = currentRevision.toBuilder()
                        .reviewers(reviewers)
                        .build();
                revisions.put(currentRevisionId, currentRevision);
            }
            return change.toBuilder()
                    .revisions(revisions)
                    .build();
        } catch (GerritClientException e) {
            log.error("process: encountered client exception while enriching change info "
                    + e.getMessage(), e);
            return change;
        }
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GerritRepositoryQuery.GerritRepositoryQueryBuilder.class)
    public static class GerritRepositoryQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("from")
        Date from;

        @JsonProperty("repos")
        List<String> repos; // fetch specific repos (format: "owner/name")

        @JsonProperty("limit")
        Integer limit;

    }
}
