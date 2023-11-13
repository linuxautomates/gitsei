package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabCommitStat;
import io.levelops.integrations.gitlab.models.GitlabProject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
public class GitlabCommitEnrichmentService {
    public GitlabCommitStat getCommitStat(GitlabClient client, GitlabProject project, String commitSha) {
        final String projectId = project.getId();
        Optional<GitlabCommit> commitWithStats = client.getCommitWithStats(projectId, commitSha);
        if (commitWithStats.isEmpty()) {
            return GitlabCommitStat.builder().build();
        }
        return commitWithStats.get().getStats();
    }

    public Stream<GitlabChange> getCommitChanges(GitlabClient client, GitlabProject project,
                                                 GitlabCommit commit, int perPage) {
        boolean fetchFileDiff = client.fetchFileDiff;
        final String projectId = project.getId();
        final String commitId = commit.getId();
        MutableInt commitChangesCount = new MutableInt(0);
        Stream<GitlabChange> changes = client.streamCommitChanges(projectId, commitId, perPage)
                .filter(Objects::nonNull)
                .peek(stateEvent -> {
                    commitChangesCount.increment();
                    if (commitChangesCount.getValue() % 50 == 0) {
                        log.info("Processed commit changes for projectId={}: commitId={}: commitsCount={} ",
                                projectId, commitId, commitChangesCount.getValue());
                    }
                });
        if (!fetchFileDiff) {
            log.info("Discarding the diffs for each of the file of commit changes for projectId={}: commitId={} ", projectId, commitId);
            changes = changes.map(change -> change.toBuilder().diff("").build());
        }
        return changes;
    }
}
