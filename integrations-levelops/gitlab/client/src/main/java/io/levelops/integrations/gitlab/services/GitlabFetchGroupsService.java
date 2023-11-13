package io.levelops.integrations.gitlab.services;

import io.levelops.commons.functional.IngestionResult;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

/**
 * This class can be used for fetching the groups.
 */
@Log4j2
public class GitlabFetchGroupsService {
    public Stream<IngestionResult<GitlabGroup>> fetchGroups(GitlabClient client, int perPage) {
        return client.streamGroups(perPage);
    }
}
