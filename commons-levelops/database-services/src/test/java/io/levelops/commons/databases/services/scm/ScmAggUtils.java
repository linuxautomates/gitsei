package io.levelops.commons.databases.services.scm;

import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.ScmAggService;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScmAggUtils {
    public static DbScmCommit createScmCommit(ScmAggService scmAggService, final String company, DbScmUser scmUser, Long now, int i) throws SQLException {
        DbScmCommit commit = DbScmCommit.builder()
                .repoIds(List.of("levelops/api-levelops")).integrationId(scmUser.getIntegrationId())
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("commit-sha-" + i)
                .committerInfo(scmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.PERFORCE)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(scmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();

        String id = scmAggService.insertCommit(company,commit);
        return commit.toBuilder().id(id).build();
    }

    public static List<DbScmCommit> createScmCommits(ScmAggService scmAggService, final String company, DbScmUser scmUser, int n) throws SQLException {
        Long now = Instant.now().getEpochSecond();
        List<DbScmCommit> commits = new ArrayList<>();
        for(int i=0; i<n; i++){
            DbScmCommit commit = createScmCommit(scmAggService, company, scmUser, now, i);
            commits.add(commit);
        }
        return commits;
    }


    public static DbScmPullRequest createPullRequest(ScmAggService scmAggService, final String company, DbScmUser scmUser, Long now, int i) throws SQLException {
        DbScmPullRequest pr = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).number(String.valueOf(i)).integrationId(scmUser.getIntegrationId())
                .project("levelops/ingestion-levelops")
                .creator("viraj-levelops").mergeSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(scmUser)
                .title("LEV-1983").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(scmUser.getIntegrationId()).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now)
                .reviews(List.of())
                .build();

        String id = scmAggService.insert(company,pr);
        return pr.toBuilder().id(id).build();
    }

    public static List<DbScmPullRequest> createPullRequests(ScmAggService scmAggService, final String company, DbScmUser scmUser, int n) throws SQLException {
        Long now = Instant.now().getEpochSecond();
        List<DbScmPullRequest> pullRequests = new ArrayList<>();
        for(int i=0; i<n; i++){
            DbScmPullRequest pullRequest = createPullRequest(scmAggService, company, scmUser, now, i);
            pullRequests.add(pullRequest);
        }
        return pullRequests;
    }
}
