package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Log4j2
public class GitReposHandler extends IntegrationDataHandler {

    private final GitRepositoryService gitRepositoryService;

    @Autowired
    public GitReposHandler(GitRepositoryService repositoryService, Storage storage,
                           ObjectMapper objectMapper) {
        super(storage, objectMapper);
        this.gitRepositoryService = repositoryService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.GITHUB;
    }

    @Override
    public String getDataType() {
        return "repositories";
    }

    @Override
    public Boolean handleStorageResult(String company, String integrationId,
                                       StorageResult storageResult) throws IOException, SQLException {
        if (!getDataType().equalsIgnoreCase(storageResult.getStorageMetadata().getDataType()))
            return false;
        List<String> dataList = getDataToPush(storageResult.getRecords());
        List<DbRepository> repoData = new ArrayList<>();
        for (String data : dataList) {
            StorageContent<ListResponse<GithubRepository>> repos = objectMapper.readValue(data,
                    StorageContent.getListStorageContentJavaType(objectMapper, GithubRepository.class));
            repos.getData().getRecords().stream()
                    .map(record ->
                            DbRepository.fromGithubRepository(record, integrationId))
                    .filter(Objects::nonNull)
                    .forEach(repoData::add);
        }
        if (repoData.size() > 0) {
            gitRepositoryService.batchUpsert(company, repoData);
        }
        log.info("Handled {} Git Repos for company: {}, integrationid: {}",
                repoData.size(), company, integrationId);
        return true;
    }
}
