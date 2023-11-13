package io.levelops.integrations.gitlab.services;

import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabBranch;
import io.levelops.integrations.gitlab.models.GitlabMilestone;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabUser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class can be used for enriching a {@link GitlabProject}.
 */
@Log4j2
public class GitlabProjectEnrichmentService {
    public Stream<GitlabProject> getProjectBranches(GitlabClient client, GitlabProject project, int perPage) {
        final String projectId = project.getId();
        MutableInt branchesCount = new MutableInt(0);
        Stream<GitlabBranch> branches = client.streamProjectBranches(projectId, perPage)
                .filter(Objects::nonNull)
                .peek(branch -> {
                    branchesCount.increment();
                    if (branchesCount.getValue() % 100 == 0) {
                        log.info("Processed Branches for projectId={}: commitsCount={} ",
                                projectId, branchesCount.getValue());
                    }
                });
        return StreamUtils.partition(branches, 200)
                .map(batch -> project.toBuilder()
                        .branches(batch)
                        .build());
    }

    public Stream<GitlabProject> getProjectUsers(GitlabClient client, GitlabProject project, Date from,
                                                 Date to, int perPage) {
        final String projectId = project.getId();
        MutableInt usersCount = new MutableInt(0);
        Stream<GitlabUser> users = client.streamUsers(projectId, from, to, perPage)
                .filter(Objects::nonNull)
                .peek(c -> {
                    usersCount.increment();
                    if (usersCount.getValue() % 100 == 0) {
                        log.info("Processed Users for projectId={}: userCount={}",
                                projectId, usersCount.getValue());
                    }
                });
        return StreamUtils.partition(users, 250)
                .map(batch -> project.toBuilder()
                        .users(batch)
                        .build());
    }

    public Stream<GitlabProject> getProjectMilestones(GitlabClient client, GitlabProject project, int perPage) {
        final String projectId = project.getId();

        MutableInt milestonesCount = new MutableInt(0);

        Stream<GitlabMilestone> milestones = client.streamMilestones(projectId, perPage)
                .filter(Objects::nonNull)
                .peek(commit -> {
                    milestonesCount.increment();
                    if (milestonesCount.getValue() % 100 == 0) {
                        log.info("Processed Milestones for projectId={}: milestonesCount={}",
                                projectId, milestonesCount.getValue());
                    }
                });
        return StreamUtils.partition(milestones, 200)
                .map(batch -> project.toBuilder()
                        .milestones(batch)
                        .build());
    }
}
