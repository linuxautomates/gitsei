package io.levelops.integrations.github.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
public class GithubProjectService {

    private final GithubClientFactory levelOpsClientFactory;

    public GithubProjectService(GithubClientFactory levelOpsClientFactory) {
        this.levelOpsClientFactory = levelOpsClientFactory;
    }

    public List<GithubProject> getProjects(IntegrationKey integrationKey, String org, Instant from, Instant to,
                                           boolean includeArchivedCards) throws GithubClientException {
        return levelOpsClientFactory.get(integrationKey, false)
                .streamProjects(org)
                .map(project -> enrichProject(integrationKey, project, from, to, includeArchivedCards))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public GithubProject getProject(IntegrationKey integrationKey, String projectId, Instant from, Instant to,
                                    boolean includeArchivedCards) throws GithubClientException {
        GithubProject project = levelOpsClientFactory.get(integrationKey, false).getProject(projectId);
        return enrichProject(integrationKey, project, from, to, includeArchivedCards);
    }

    public GithubProject enrichProject(IntegrationKey integrationKey, GithubProject project, Instant from, Instant to,
                                       boolean includeArchivedCards) {
        if (project == null || project.getId() == null) {
            return project;
        }
        List<GithubProjectColumn> columns = null;
        try {
            columns = getProjectColumns(integrationKey, project.getId());
        } catch (GithubClientException e) {
            log.warn("enrichProject: Failed to get project columns of project {} for integration {}",
                    project.getId(), integrationKey, e);
        }
        List<GithubProjectColumn> enrichedColumns = null;
        if (columns != null) {
            enrichedColumns = columns.stream()
                    .map(column -> enrichProjectColumn(integrationKey, column, from, to, includeArchivedCards))
                    .collect(Collectors.toList());
        }
        return project.toBuilder()
                .columns(enrichedColumns)
                .build();
    }

    public List<GithubProjectColumn> getProjectColumns(IntegrationKey integrationKey, String projectId)
            throws GithubClientException {
        GithubClient client = levelOpsClientFactory.get(integrationKey, false);
        return client.streamProjectColumns(projectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public GithubProjectColumn enrichProjectColumn(IntegrationKey integrationKey, GithubProjectColumn column,
                                                   Instant from, Instant to, boolean includeArchivedCards) {
        if (column == null || column.getId() == null) {
            return null;
        }
        List<GithubProjectCard> cards = null;
        try {
            cards = getProjectColumnsCards(integrationKey, column.getId(), from, to, includeArchivedCards);
        } catch (GithubClientException e) {
            log.warn("enrichProjectColumn: Failed to get cards of project column {} for integration {}",
                    column.getId(), integrationKey, e);
        }
        return column.toBuilder()
                .cards(cards)
                .build();
    }

    public List<GithubProjectCard> getProjectColumnsCards(IntegrationKey integrationKey, String columnId,
                                                          Instant from, Instant to, boolean includeArchivedCards) throws GithubClientException {
        GithubClient client = levelOpsClientFactory.get(integrationKey, false);
        return client.streamProjectColumnCards(columnId, includeArchivedCards)
                .filter(Objects::nonNull)
                .filter(card -> includeArchivedCards || (card.getUpdatedAt() != null &&
                        card.getUpdatedAt().toInstant().isAfter(from) &&
                        card.getUpdatedAt().toInstant().isBefore(to)))
                .collect(Collectors.toList());
    }

}
