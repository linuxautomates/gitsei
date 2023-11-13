package io.levelops.integrations.harnessng.source;

import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.harnessng.client.HarnessNGClient;
import io.levelops.integrations.harnessng.client.HarnessNGClientException;
import io.levelops.integrations.harnessng.client.HarnessNGClientFactory;
import io.levelops.integrations.harnessng.models.HarnessNGExecutionInputSet;
import io.levelops.integrations.harnessng.models.HarnessNGIngestionQuery;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGProject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HarnessNG's implementation for a {@Link DataSource}. It is responsible to fetch Repository data.
 */
@Log4j2
public class HarnessNGEnrichPipelineDataSource implements DataSource<HarnessNGPipelineExecution, HarnessNGIngestionQuery> {

    private final HarnessNGClientFactory harnessngClientFactory;

    public HarnessNGEnrichPipelineDataSource(HarnessNGClientFactory harnessngClientFactory) {
        this.harnessngClientFactory = harnessngClientFactory;
    }

    @Override
    public Data<HarnessNGPipelineExecution> fetchOne(HarnessNGIngestionQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<HarnessNGPipelineExecution>> fetchMany(HarnessNGIngestionQuery query) throws FetchException {
        HarnessNGClient harnessngClient = harnessngClientFactory.get(query.getIntegrationKey());
        return getProjectsData(harnessngClient, query.getFrom(), query.getTo(), query.getOrganizations(), query.getProjects(), query.getAccountIdentifier());
    }

    private Stream<Data<HarnessNGPipelineExecution>> getProjectsData(HarnessNGClient client, Date from, Date to, List<String> organizations, List<String> projects, String accountIdentifier) {
        List<String> finalOrgs = sanitizeOrgs(organizations, projects);
        Stream<Data<HarnessNGPipelineExecution>> pipelines = client.streamProjects(accountIdentifier)
                .filter(project -> filterProject(finalOrgs, projects, project))
                .flatMap(project -> getEnrichedPipelineExecutionsInProject(client, project, from, to, accountIdentifier))
                .map(BasicData.mapper(HarnessNGPipelineExecution.class));
        return pipelines.filter(Objects::nonNull);
    }

    @Nonnull
    private static List<String> sanitizeOrgs(List<String> organizations, List<String> projects) {
        // remove orgs that have specific projects selected
        // so that only those projects get scanned
        return ListUtils.emptyIfNull(organizations).stream()
                .filter(StringUtils::isNotBlank)
                .filter(org -> ListUtils.emptyIfNull(projects).stream()
                        .filter(StringUtils::isNotBlank)
                        .noneMatch(project -> project.contains(org + "/")))
                .collect(Collectors.toList());
    }

    private static boolean filterProject(List<String> orgs, List<String> projects, HarnessNGProject project) {
        return (ListUtils.isEmpty(orgs) && ListUtils.isEmpty(projects))
                || orgs.contains(project.getProject().getOrgIdentifier())
                || projects.contains(project.getProject().getOrgIdentifier() + "/" + project.getProject().getIdentifier());
    }

    private Stream<HarnessNGPipelineExecution> getEnrichedPipelineExecutionsInProject(HarnessNGClient client,
                                                                                      HarnessNGProject project,
                                                                                      Date from,
                                                                                      Date to,
                                                                                      String accountIdentifier) {
        String projectIdentifier = project.getProject().getIdentifier();
        String orgIdentifier = project.getProject().getOrgIdentifier();
        return client.streamExecutions(accountIdentifier, projectIdentifier, orgIdentifier, from.getTime(), to.getTime())
                .filter(pipeline -> pipeline.getEndTs() != null && Instant.ofEpochMilli(pipeline.getEndTs()).compareTo(Instant.ofEpochMilli(from.getTime())) >= 0)
                .filter(pipeline -> Instant.ofEpochMilli(pipeline.getEndTs()).isBefore(Instant.ofEpochMilli(to.getTime())))
                .map(pipeline -> enrichPipelineExecution(client, accountIdentifier, orgIdentifier, projectIdentifier, pipeline.getExecutionId()));
    }

    private HarnessNGPipelineExecution enrichPipelineExecution(HarnessNGClient client,
                                                               String accountIdentifier,
                                                               String orgIdentifier,
                                                               String projectIdentifier,
                                                               String executionId) {
        // fetch execution details
        HarnessNGPipelineExecution execution = client.getPipelineExecutionDetails(accountIdentifier, projectIdentifier, orgIdentifier, executionId, true);
        if (execution.getPipeline() == null) {
            log.warn("Ignoring invalid execution with no pipeline metadata: {}", execution);
            return null;
        }

        // fetch input set (i.e. run parameters)
        HarnessNGExecutionInputSet inputSet = null;
        try {
            inputSet = client.getExecutionInputSet(accountIdentifier, projectIdentifier, orgIdentifier, executionId, true);
        } catch (HarnessNGClientException e) {
            log.warn("Failed to fetch input set for executionId={}", executionId, e);
        }

        return execution.toBuilder()
                .pipeline(execution.getPipeline().toBuilder()
                        .orgIdentifier(orgIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .build())
                .inputSet(inputSet)
                .build();
    }
}
