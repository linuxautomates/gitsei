package io.levelops.integrations.gerrit.services;

import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

@Log4j2
public class GerritFetchProjectsService {

    public Stream<ProjectInfo> fetchRepos(GerritClient client) {
        return client.streamProjects();
    }
}
