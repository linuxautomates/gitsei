package io.levelops.integrations.gitlab.services;

import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabTag;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

@Log4j2
public class GitlabFetchTagService {

    public Stream<GitlabProject> getProjectTags(GitlabClient client, GitlabProject project, int perPage) {
        String projectId = project.getId();
        Stream<GitlabTag> tags = client.streamTags(projectId, perPage);
        return StreamUtils.partition(tags, 200)
                .map(batch -> project.toBuilder()
                        .tags(batch)
                        .build());
    }
}