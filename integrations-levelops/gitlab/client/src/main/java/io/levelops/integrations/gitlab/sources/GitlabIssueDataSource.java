package io.levelops.integrations.gitlab.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabIssueNote;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import io.levelops.integrations.gitlab.services.GitlabFetchIssueNotesService;
import io.levelops.integrations.gitlab.services.GitlabFetchIssuesService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.Date;
import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gitlab's implementation of the {@link DataSource}. This class can be used to fetch {@link GitlabIssue}
 * data from Gitlab.
 */
@Log4j2
public class GitlabIssueDataSource implements DataSource<GitlabIssue, GitlabQuery> {
    private final static int DEFAULT_PER_PAGE = 100;
    private final static int MAX_NOTES = 100;
    private final EnumSet<Enrichment> enrichments;
    private final GitlabFetchIssuesService fetchIssuesService;
    private final GitlabFetchIssueNotesService fetchIssueNotesService;
    private final GitlabClientFactory clientFactory;

    public GitlabIssueDataSource(GitlabClientFactory clientFactory) {
        this(clientFactory, EnumSet.noneOf(Enrichment.class));
    }


    public GitlabIssueDataSource(GitlabClientFactory clientFactory, EnumSet<Enrichment> enrichments) {
        this.enrichments = enrichments;
        this.clientFactory = clientFactory;
        fetchIssuesService = new GitlabFetchIssuesService();
        fetchIssueNotesService = new GitlabFetchIssueNotesService();
    }

    private GitlabClient getClient(IntegrationKey integrationKey) throws FetchException {
        GitlabClient client;
        try {
            client = clientFactory.get(integrationKey, false);
        } catch (GitlabClientException e) {
            throw new FetchException("Could not fetch Gitlab client", e);
        }
        return client;
    }

    @Override
    public Data<GitlabIssue> fetchOne(GitlabQuery query) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<GitlabIssue>> fetchMany(GitlabQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();
        Date lastActivityAfter = query.getFrom();
        Date lastActivityBefore = query.getTo();
        GitlabClient client = getClient(integrationKey);
        Stream<Data<GitlabIssue>> stream = fetchIssuesService.fetchIssues(client, lastActivityAfter,
                lastActivityBefore, DEFAULT_PER_PAGE)
                .filter(Objects::nonNull)
                .map(gitlabIssues -> parseAndEnrichIssue(client, String.valueOf(gitlabIssues.getProjectId()), gitlabIssues))
                .map(BasicData.mapper(GitlabIssue.class));
        return stream.filter(Objects::nonNull);
    }

    private GitlabIssue parseAndEnrichIssue(GitlabClient client,
                                            String projectId,
                                            GitlabIssue issue) {
        Stream<GitlabIssueNote> issueNotes = Stream.empty();
        if (enrichments.contains(Enrichment.NOTES)) {
            issueNotes = fetchIssueNotesService.getIssueNotes(client, projectId, issue, DEFAULT_PER_PAGE);
        }
        return issue.toBuilder()
                .notes(issueNotes.limit(MAX_NOTES).collect(Collectors.toList()))
                .build();
    }

    public enum Enrichment {
        NOTES
    }
}

