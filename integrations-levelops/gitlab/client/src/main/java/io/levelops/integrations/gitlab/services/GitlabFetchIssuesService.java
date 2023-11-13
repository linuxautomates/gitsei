package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.stream.Stream;

/**
 * This class can be used for fetching {@link GitlabIssue}.
 */
@Log4j2
public class GitlabFetchIssuesService {
    public Stream<GitlabIssue> fetchIssues(GitlabClient client, Date from, Date to, int perPage) {
        return client.streamIssues(from, to, perPage);
    }
}
