package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabProject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * This class can be used for fetching {@link GitlabProject}
 */
@Log4j2
public class GitlabFetchProjectsService {

    public Stream<GitlabProject> fetchProjects(GitlabClient client, boolean checkMembership) {
        return client.streamProjects(checkMembership);
    }

    public Stream<GitlabProject> getProjectStream(
            GitlabClient client,
            List<String> projectNamesToInclude,
            List<String> projectIdsToExclude,
            boolean checkMembership,
            String resumeFromRepo
    ) {
        MutableBoolean skipRepos = new MutableBoolean(false);
        if (StringUtils.isNotBlank(resumeFromRepo)) {
            log.info("Will skip repos before '{}'", resumeFromRepo);
            skipRepos.setTrue();
        }
        return fetchProjects(client, checkMembership)
                .filter(Objects::nonNull)
                .filter(project -> {
                    if (CollectionUtils.isNotEmpty(projectNamesToInclude)) {
                        return projectNamesToInclude.contains(project.getName());
                    } else if (CollectionUtils.isNotEmpty(projectIdsToExclude) &&
                            projectIdsToExclude.contains(project.getId())) {
                        return false;
                    } else {
                        return true;
                    }
                })
                .filter(project -> {
                    try {
                        if (skipRepos.isFalse()) {
                            return true;
                        } else {
                            String projectName = project.getNameWithNamespace();
                            if (projectName.equalsIgnoreCase(resumeFromRepo)) {
                                log.info("Resuming scan from repo={}", resumeFromRepo);
                                skipRepos.setFalse();
                                return true;
                            } else {
                                log.debug(">>> skipping repo {}", projectName);
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to filter skipped repos");
                        throw e;
                    }
                });
    }
}
