package io.levelops.integrations.droneci.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.droneci.client.DroneCIClient;
import io.levelops.integrations.droneci.client.DroneCIClientException;
import io.levelops.integrations.droneci.client.DroneCIClientFactory;
import io.levelops.integrations.droneci.models.DroneCIIngestionQuery;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import io.levelops.integrations.droneci.models.DroneCIBuild;
import io.levelops.integrations.droneci.models.DroneCIBuildStage;
import io.levelops.integrations.droneci.models.DroneCIBuildStep;
import io.levelops.integrations.droneci.models.DroneCIBuildStepLog;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DroneCI's implementation for a {@Link DataSource}. It is responsible to fetch Repository data.
 */
@Log4j2
public class DroneCIEnrichRepoDataSource implements DataSource<DroneCIEnrichRepoData, DroneCIIngestionQuery> {

    private final DroneCIClientFactory droneCIClientFactory;
    private Boolean fetchStepLogs;
    public static long MAX_STAGE_SIZE = 1048576; // 1 MB
    public static String SUCCESS_STATUS = "success";

    @Autowired
    private ObjectMapper objectMapper;

    public DroneCIEnrichRepoDataSource(DroneCIClientFactory droneCIClientFactory) {
        this.droneCIClientFactory = droneCIClientFactory;
        this.fetchStepLogs = false;
    }

    @Override
    public Data<DroneCIEnrichRepoData> fetchOne(DroneCIIngestionQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<DroneCIEnrichRepoData>> fetchMany(DroneCIIngestionQuery query) throws FetchException {
        DroneCIClient droneCIClient = droneCIClientFactory.get(query.getIntegrationKey());
        fetchStepLogs = query.getShouldFetchStepLogs();
        return getRepositoriesData(droneCIClient, query.getFrom(), query.getTo(), query.getRepositories(), query.getExcludeRepositories());
    }

    private Stream<Data<DroneCIEnrichRepoData>> getRepositoriesData(DroneCIClient client, Date from, Date to, List<String> repos, List<String> excludeRepos) throws DroneCIClientException {
        RepoFilterPredicate repoFilterPredicate = new RepoFilterPredicate(repos, excludeRepos);
        Stream<Data<DroneCIEnrichRepoData>> repositories = client.streamRepositories()
                .filter(repoFilterPredicate)
                .flatMap(repo -> getRepoEnrichBuilds(client, repo, from, to))
                .map(BasicData.mapper(DroneCIEnrichRepoData.class));
        return repositories.filter(Objects::nonNull);
    }

    public static class RepoFilterPredicate implements Predicate<DroneCIEnrichRepoData> {
       private final List<String> repos;
        private final List<String> excludeRepos;
        RepoFilterPredicate(List<String> repos, List<String> excludeRepos){
           this.repos = repos;
           this.excludeRepos = excludeRepos;
        }
        @Override
        public boolean test(DroneCIEnrichRepoData repo) {
            if (ListUtils.isEmpty(repos) && ListUtils.isEmpty(excludeRepos)) {
                return true;
            } else if (!ListUtils.isEmpty(repos) && !ListUtils.isEmpty(excludeRepos)) {
                return repos.contains(repo.getSlug()) && !excludeRepos.contains(repo.getSlug());
            } else if (!ListUtils.isEmpty(excludeRepos)) {
                return !excludeRepos.contains(repo.getSlug());
            } else {
                return repos.contains(repo.getSlug());
            }
        }
    }

    private Stream<DroneCIEnrichRepoData> getRepoEnrichBuilds(DroneCIClient client, DroneCIEnrichRepoData repo, Date from, Date to) {
        Stream<DroneCIBuild> builds = client.streamRepoBuilds(repo.getNamespace(), repo.getName())
                .filter(build -> Instant.ofEpochSecond(build.getFinished()).compareTo(Instant.ofEpochMilli(from.getTime())) >= 0)
                .filter(build -> Instant.ofEpochSecond(build.getFinished()).isBefore(Instant.ofEpochMilli(to.getTime())))
                .map(build -> getBuildEnrichment(client, build, repo.getNamespace(), repo.getName(), repo.getSlug()));
        return StreamUtils.partition(builds, 50)
                .map(batch -> repo.toBuilder()
                        .builds(batch)
                        .build());
    }

    private DroneCIBuild getBuildEnrichment(DroneCIClient client, DroneCIBuild build, String repoOwner, String repoName, String slug) {
        List<DroneCIBuildStage> buildStages = List.of();
        try {
            buildStages = ListUtils.emptyIfNull(client.getBuildInfo(repoOwner, repoName, build.getNumber()).getStages())
                    .stream()
                    .map(stage -> getBuildStage(client, build.getNumber(), stage, repoOwner, repoName)
                            .toBuilder()
                            .stageUrl(client.getResourceUrl() + "/" + slug + "/" + build.getNumber())
                            .build())
                    .collect(Collectors.toList());
        } catch (DroneCIClientException e) {
            log.error("getBuildEnrichment: encountered DroneCI client error as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered DroneCI client exception :", e);
        }
        return build.toBuilder().stages(buildStages).build();
    }

    private DroneCIBuildStage getBuildStage(DroneCIClient client, long buildNumber, DroneCIBuildStage stage, String repoOwner, String repoName) {
        final long stageSize = getSizeOfObject(stage);
        List<DroneCIBuildStep> steps = ListUtils.emptyIfNull(stage.getSteps())
                .stream()
                .map(step -> getBuildStepLogs(client, step, repoOwner, repoName, buildNumber, stage.getNumber(), stageSize))
                .collect(Collectors.toList());

        return stage.toBuilder().steps(steps).build();
    }

    private DroneCIBuildStep getBuildStepLogs(DroneCIClient client, DroneCIBuildStep step, String repoOwner, String repoName, long buildNumber, long stageNumber, final long finalStageSize) {
        if (!fetchStepLogs || SUCCESS_STATUS.equals(step.getStatus())) {
            return step;
        }
        long stageSize = finalStageSize;
        List<DroneCIBuildStepLog> stepLogs = client.buildStepLogs(repoOwner, repoName, buildNumber, stageNumber, step.getNumber());
        List<DroneCIBuildStepLog> updatedLogs = new ArrayList<>();
        for(DroneCIBuildStepLog log: stepLogs){
            long logSize = log.getOut().getBytes(StandardCharsets.UTF_8).length;
            if(stageSize + logSize <= MAX_STAGE_SIZE){
                updatedLogs.add(log);
                stageSize += logSize;
            }
        }
        return step.toBuilder().stepLogs(updatedLogs).build();
    }

    private long getSizeOfObject(Object object){
        try{
            String serialized = objectMapper.writeValueAsString(object);
            return serialized.getBytes(StandardCharsets.UTF_8).length;
        }
        catch (JsonProcessingException e){
            log.error("Failed to get size of object", e);
        }
        return 0;
    }

}
