package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabJob;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * This class can be used for enriching a {@link GitlabPipeline} with {@link GitlabJob}.
 */
@Log4j2
public class GitlabFetchJobsService {


    public Stream<GitlabJob> getPipelineJobs(GitlabClient client, GitlabProject project,
                                             GitlabPipeline pipeline, int perPage) {
        final String projectId = project.getId();
        final String pipelineId = pipeline.getPipelineId();
        MutableInt jobsCount = new MutableInt(0);
        return client.streamJobs(projectId, pipelineId, perPage)
                .filter(Objects::nonNull)
                .peek(gitlabJob -> {
                    jobsCount.increment();
                    if (jobsCount.getValue() % 50 == 0) {
                        log.info("Processed state jobs  for projectId={}: pipelineId={}: commitsCount={} ",
                                projectId, pipelineId, jobsCount.getValue());
                    }
                });
    }
}

