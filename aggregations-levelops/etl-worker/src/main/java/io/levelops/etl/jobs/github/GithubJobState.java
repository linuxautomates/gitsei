package io.levelops.etl.jobs.github;

import io.levelops.commons.databases.models.database.repo.DbRepository;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GithubJobState {
    List<String> productIds;
    List<DbRepository> repositoryList;
}
