package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.UserIdentityMaskingService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Service
@Log4j2
public class JiraUsersHandler extends IntegrationDataHandler {

    private final JiraIssueService jiraIssueService;
    private final UserIdentityService userIdentityService;
    private final UserIdentityMaskingService userIdentityMaskingService;

    @Autowired
    public JiraUsersHandler(DataSource dataSource, JiraIssueService jiraIssueService, Storage storage, ObjectMapper objectMapper,UserIdentityMaskingService userIdentityMaskingService) {
        super(storage, objectMapper);
        this.jiraIssueService = jiraIssueService;
        this.userIdentityService = new UserIdentityService(dataSource);
        this.userIdentityMaskingService= userIdentityMaskingService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }

    @Override
    public String getDataType() {
        return "users";
    }

    @Override
    public Boolean handleStorageResult(String company, String integrationId, StorageResult storageResult)
            throws IOException {
        if (!getDataType().equalsIgnoreCase(storageResult.getStorageMetadata().getDataType()))
            return false;
        List<String> dataList = getDataToPush(storageResult.getRecords());
        for (String data : dataList) {
            StorageContent<ListResponse<JiraUser>> fields = objectMapper.readValue(data,
                    StorageContent.getListStorageContentJavaType(objectMapper, JiraUser.class));
            fields.getData().getRecords().stream()
                    .map(record -> DbJiraUser.fromJiraUser(record, integrationId))
                    .filter(Objects::nonNull)
                    .forEach(user -> {
                        jiraIssueService.insertJiraUser(company, user);
                        if (user.getDisplayName() != null) {
                            try {
                                String maskedUser=null;
                                boolean isMasked=userIdentityMaskingService.isMasking(company,integrationId,user.getJiraId(),user.getDisplayName());
                                if(isMasked){
                                    maskedUser=userIdentityMaskingService.maskedUser(company);
                                }else{
                                    maskedUser =user.getDisplayName();
                                }
                                userIdentityService.batchUpsert(company,
                                        List.of(DbScmUser.builder()
                                                .integrationId(user.getIntegrationId())
                                                .cloudId(user.getJiraId())
                                                .displayName(maskedUser)
                                                .originalDisplayName(user.getDisplayName())
                                                .build()));
                            } catch (SQLException throwables) {
                                log.error("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + company + ", integration id:" + integrationId);
                            }
                        }
                    });
        }
        log.info("Finished Jira Users for company: {}, integrationid: {}", company, integrationId);
        return true;
    }
}
