package io.levelops.scm.services;

import io.levelops.commons.databases.models.database.scm.DbScmCommit;

import java.util.List;
import java.util.Set;

public interface ScmService {
    public DbScmCommit getCommit(final String company, final String commitSha, final String repoId, final Integer integrationId);
    public Set<DbScmCommit> listCommits(final String company, final List<String> commitShas, final String repoId);
}