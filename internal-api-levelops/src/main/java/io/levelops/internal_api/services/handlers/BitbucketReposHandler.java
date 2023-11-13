package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.services.BitbucketRepositoryService;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Log4j2
public class BitbucketReposHandler extends IntegrationDataHandler {
    private static final Log LOGGER = LogFactory.getLog(MethodHandles.lookup().lookupClass());

    private final BitbucketRepositoryService repositoryService;

    @Autowired
    public BitbucketReposHandler(BitbucketRepositoryService repositoryService, Storage storage,
                           ObjectMapper objectMapper) {
        super(storage, objectMapper);
        this.repositoryService = repositoryService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.BITBUCKET;
    }

    @Override
    public String getDataType() {
        return "commits";
    }

    @Override
    public Boolean handleStorageResult(String company, String integrationId,
                                       StorageResult storageResult) throws IOException, SQLException {
        if (!getDataType().equalsIgnoreCase(storageResult.getStorageMetadata().getDataType())) {
            LOGGER.warn("Handler data type " + getDataType() + " is not equal to storageResult data type " + storageResult.getStorageMetadata().getDataType() + " not storing result!");
            return false;
        }
        List<String> dataList = getDataToPush(storageResult.getRecords());
        List<DbRepository> repoData = new ArrayList<>();
        for (String data : dataList) {
            StorageContent<ListResponse<BitbucketRepository>> repos = objectMapper.readValue(data,
                    StorageContent.getListStorageContentJavaType(objectMapper, BitbucketRepository.class));
            repos.getData().getRecords().stream()
                    .map(record -> DbRepository.fromBitbucketRepository(record, integrationId))
                    .filter(Objects::nonNull)
                    .forEach(repoData::add);
        }
        if (repoData.size() > 0) {
            repositoryService.batchUpsert(company, repoData);
        }
        log.info("Handled {} Bitbucket Repos for company: {}, integrationid: {}",
                repoData.size(), company, integrationId);
        return true;
    }
}
