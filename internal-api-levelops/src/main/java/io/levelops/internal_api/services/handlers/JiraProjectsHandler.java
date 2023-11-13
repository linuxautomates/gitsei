package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.jira.models.JiraProject;
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
public class JiraProjectsHandler extends IntegrationDataHandler {

    private final JiraProjectService projectService;

    @Autowired
    public JiraProjectsHandler(JiraProjectService projectService, Storage storage,
                               ObjectMapper objectMapper) {
        super(storage, objectMapper);
        this.projectService = projectService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }

    @Override
    public String getDataType() {
        return "projects";
    }

    @Override
    public Boolean handleStorageResult(String company, String integrationId,
                                       StorageResult storageResult) throws IOException, SQLException {
        if (!getDataType().equalsIgnoreCase(storageResult.getStorageMetadata().getDataType()))
            return false;
        List<String> dataList = getDataToPush(storageResult.getRecords());
        List<DbJiraProject> projData = new ArrayList<>();
        for (String data : dataList) {
            StorageContent<ListResponse<JiraProject>> projects = objectMapper.readValue(data,
                    StorageContent.getListStorageContentJavaType(objectMapper, JiraProject.class));
            projects.getData().getRecords().stream()
                    .map(record -> DbJiraProject.fromJiraProject(record, integrationId))
                    .filter(Objects::nonNull)
                    .forEach(projData::add);
        }
        if (projData.size() > 0) {
            projectService.batchUpsert(company, projData);
        }
        log.info("Handled {} Jira Projects for company: {}, integrationid: {}",
                projData.size(), company, integrationId);
        return true;
    }
}
