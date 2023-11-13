package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.models.ScmRepository;

import java.util.List;

public interface ScmRepositoryService {

    int getTotalRepositoriesCount(String company, Integration integration, String repoName, String projectKey) throws Exception;

    List<ScmRepository> getScmRepositories(String company, Integration integration, List<String> filterRepos, int pageNumber, int pageSize) throws Exception;

    List<ScmRepository> searchScmRepository(String company, Integration integration, String repoName, String projectKey, int pageNumber, int pageSize) throws Exception;
}
