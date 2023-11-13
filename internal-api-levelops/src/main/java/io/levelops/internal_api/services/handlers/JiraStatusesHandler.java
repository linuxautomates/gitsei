package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraStatus;
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
public class JiraStatusesHandler extends IntegrationDataHandler {

    private final JiraStatusMetadataDatabaseService statusMetadataDatabaseService;

    @Autowired
    public JiraStatusesHandler(JiraStatusMetadataDatabaseService statusMetadataDatabaseService, Storage storage, ObjectMapper objectMapper) {
        super(storage, objectMapper);
        this.statusMetadataDatabaseService = statusMetadataDatabaseService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }

    @Override
    public String getDataType() {
        return "statuses";
    }

    @Override
    public Boolean handleStorageResult(String company, String integrationId, StorageResult storageResult) throws IOException {
        if (!getDataType().equalsIgnoreCase(storageResult.getStorageMetadata().getDataType()))
            return false;
        List<String> dataList = getDataToPush(storageResult.getRecords());
        List<DbJiraStatusMetadata> outputData = new ArrayList<>();
        for (String data : dataList) {
            StorageContent<ListResponse<JiraStatus>> records = objectMapper.readValue(data,
                    StorageContent.getListStorageContentJavaType(objectMapper, JiraStatus.class));
            records.getData().getRecords().stream()
                    .map(record -> DbJiraStatusMetadata.fromJiraStatus(integrationId, record))
                    .filter(Objects::nonNull)
                    .forEach(outputData::add);
        }
        outputData.forEach(status -> {
            try {
                statusMetadataDatabaseService.upsert(company, status);
            } catch (SQLException e) {
                log.warn("Failed to insert Jira status metadata for company={}, integrationId={}", company, integrationId, e);
            }
        });
        log.info("Handled {} Jira Statuses Metadata for company={}, integrationId={}",
                outputData.size(), company, integrationId);
        return true;
    }
}
