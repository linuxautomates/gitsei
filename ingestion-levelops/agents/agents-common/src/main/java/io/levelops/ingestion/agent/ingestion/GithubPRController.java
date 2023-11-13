package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.github.models.GithubGetPRResult;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.github.GithubDataSource;
import io.levelops.integrations.github.model.GithubData;
import io.levelops.ingestion.integrations.github.models.GithubQuery;
import lombok.Builder;

public class GithubPRController implements DataController<GithubQuery> {

    private final ObjectMapper objectMapper;
    private final GithubDataSource githubDataSource;

    @Builder
    public GithubPRController(ObjectMapper objectMapper, GithubDataSource githubDataSource) {
        this.objectMapper = objectMapper;
        this.githubDataSource = githubDataSource;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GithubQuery query) throws IngestException {
        Data<GithubData> githubData = githubDataSource.fetchOne(GithubDataSource.GithubQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .repoName(query.getRepoName())
                .repoOwner(query.getRepoOwner())
                .prNumber(query.getPrNumber())
                .build());
        return GithubGetPRResult.builder()
                .pullRequest(githubData.getPayload().getPullRequest())
                .build();
    }

    @Override
    public GithubQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GithubQuery.class);
    }
}
