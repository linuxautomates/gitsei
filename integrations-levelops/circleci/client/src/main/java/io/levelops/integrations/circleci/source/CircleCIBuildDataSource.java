package io.levelops.integrations.circleci.source;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.circleci.client.CircleCIClient;
import io.levelops.integrations.circleci.client.CircleCIClientException;
import io.levelops.integrations.circleci.client.CircleCIClientFactory;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.integrations.circleci.models.CircleCIBuildStep;
import io.levelops.integrations.circleci.models.CircleCIStepAction;
import io.levelops.integrations.circleci.models.CircleCIStepActionLog;
import io.levelops.integrations.circleci.models.CircleCIIngestionQuery;
import io.levelops.integrations.circleci.models.CircleCIProject;
import io.levelops.integrations.circleci.services.CircleCIBuildEnrichmentService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CircleCI's implementation for a {@link DataSource}. It is responsible to fetch Build data as per its pagination logic.
 */
@Log4j2
public class CircleCIBuildDataSource implements DataSource<CircleCIBuild, CircleCIIngestionQuery> {

    private static final String FAILURE_STATE = "failed";

    private final CircleCIClientFactory circleCIClientFactory;
    private final CircleCIBuildEnrichmentService enrichmentService;

    private Boolean fetchActionLogs;
    private Map<String, Pair<String, String>> usernameRepoMappings;

    public CircleCIBuildDataSource(CircleCIClientFactory circleCIClientFactory,
                                   CircleCIBuildEnrichmentService enrichmentService) {
        this.circleCIClientFactory = circleCIClientFactory;
        this.enrichmentService = enrichmentService;
        this.fetchActionLogs = false;
        this.usernameRepoMappings = new HashMap<>();
    }

    @Override
    public Data<CircleCIBuild> fetchOne(CircleCIIngestionQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<CircleCIBuild>> fetchMany(CircleCIIngestionQuery query) throws FetchException {
        CircleCIClient circleCIClient = circleCIClientFactory.get(query.getIntegrationKey());
        fetchActionLogs = query.getShouldFetchActionLogs();
        try {
            List<CircleCIProject> projects = circleCIClient.getProjects();
            for (CircleCIProject project : projects) {
                String[] segments = project.getVcsUrl().split("/");
                if (segments.length == 5) {
                    usernameRepoMappings.put(segments[3] + "/" + segments[4], Pair.of(project.getUsername(), project.getReponame()));
                }
            }
        } catch (CircleCIClientException e) {
            log.error("fetchMany: encountered CircleCI client error for getting projects: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
        }
        return getBuilds(circleCIClient, query);
    }

    private Stream<Data<CircleCIBuild>> getBuilds(CircleCIClient circleCIClient, CircleCIIngestionQuery query) {
        List<CircleCIBuild> recentBuilds = circleCIClient.streamBuilds()
                .map(build -> {
                    Pair<String, String> usernameRepo = usernameRepoMappings.get(build.getUsername() + "/" + build.getRepoName());
                    if (usernameRepo != null) {
                        return build.toBuilder()
                                .user(usernameRepo.getLeft())
                                .repo(usernameRepo.getRight())
                                .build();
                    } else {
                        return build;
                    }
                })
                .filter(build -> ListUtils.emptyIfNull(query.getRepositories()).isEmpty() || query.getRepositories().contains(build.getSlug()) || query.getRepositories().contains(build.getModifiedSlug()))
                .filter(build -> build.getStartTime() != null) //filter out build that do NOT have start time
                .filter(build -> {
                    //Stop Time is not NULL and is within range
                    boolean stopTimeIsInRange = (build.getStopTime() != null) && ( (build.getStopTime().compareTo(query.getFrom()) >= 0) && (build.getStopTime().compareTo(query.getTo()) <= 0) );
                    return stopTimeIsInRange;
                })
                .collect(Collectors.toList());
        recentBuilds = enrichmentService.enrichBuilds(circleCIClient, recentBuilds).stream().map(build -> getBuild(circleCIClient, build)).collect(Collectors.toList());
        return recentBuilds.stream().map(BasicData.mapper(CircleCIBuild.class));
    }

    private CircleCIBuild getBuild(CircleCIClient client, CircleCIBuild build) {
        List<CircleCIBuildStep> steps = ListUtils.emptyIfNull(build.getSteps())
                .stream()
                .map(step -> getStep(client, step))
                .collect(Collectors.toList());
        return build.toBuilder().steps(steps).build();
    }

    private CircleCIBuildStep getStep(CircleCIClient client, CircleCIBuildStep step) {
        List<CircleCIStepAction> actions = ListUtils.emptyIfNull(step.getActions())
                .stream()
                .map(action -> getAction(client, action))
                .collect(Collectors.toList());
        return step.toBuilder().actions(actions).build();
    }

    private CircleCIStepAction getAction(CircleCIClient client, CircleCIStepAction action) {
        if (!fetchActionLogs) {
            return action;
        }
        if (action == null) {
            return null;
        }
        String outputUrl = action.getOutputUrl();
        if (StringUtils.isEmpty(outputUrl)) {
            return action;
        }
        try {
            if (StringUtils.equalsIgnoreCase(FAILURE_STATE, action.getStatus()) ||
                    (action.getExitCode() != null && action.getExitCode() != 0)) {
                List<CircleCIStepActionLog> actionLogs = client.getActionLogs(outputUrl);
                return action.toBuilder().actionLogs(actionLogs).build();

            } else {
                log.debug("addLogs: skipping log for action: {}", action.getName());
                return action;
            }
        } catch (CircleCIClientException e) {
            log.warn("addLogs: error fetching log for: " + outputUrl, e);
            return action;
        }
    }

}
