package io.levelops.etl.jobs.jira;

import io.levelops.aggregations.services.CustomFieldService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.jira.models.JiraField;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraFieldsStage extends BaseIngestionResultProcessingStage<JiraField, JiraJobState> {
    private final JiraFieldService jiraFieldService;
    private final CustomFieldService customFieldService;

    protected JiraFieldsStage(JiraFieldService jiraFieldService,
                              CustomFieldService customFieldService) {
        this.jiraFieldService = jiraFieldService;
        this.customFieldService = customFieldService;
    }

    @Override
    public String getName() {
        return "Jira Fields Stage";
    }

    @Override
    public void preStage(JobContext context, JiraJobState jobState) throws SQLException {
        Map<String, DbJiraField>  existingFields = jiraFieldService.listByFilter(
                context.getTenantId(),
                List.of(context.getIntegrationId()),
                null,
                null,
                null,
                null,
                0,
                1000000).getRecords().stream()
                .collect(Collectors.toMap(DbJiraField::getFieldKey, Function.identity()));
        jobState.setExistingFields(existingFields);
        jobState.setNewFields(new ArrayList<>());
    }

    @Override
    public void process(JobContext context, JiraJobState jobState, String ingestionJobId, JiraField entity) throws SQLException {
        DbJiraField field = DbJiraField.fromJiraField(entity, context.getIntegrationId());
        jobState.getNewFields().add(field);
    }

    @Override
    public void postStage(JobContext context, JiraJobState jobState) {
        List<DbJiraField> filteredFields = jobState.getNewFields().stream()
                .filter(Objects::nonNull)
                .filter(record -> {
                    // if nothing is changed with the record, no need to attempt to insert.
                    if (!jobState.existingFields.containsKey(record.getFieldKey())) {
                        return true;
                    }
                    DbJiraField existingField = jobState.existingFields.get(record.getFieldKey());
                    if (!StringUtils.equals(existingField.getName(), record.getName())) {
                        return true;
                    }
                    if (!StringUtils.equals(existingField.getFieldType(), record.getFieldType())) {
                        return true;
                    }
                    return !StringUtils.equals(existingField.getFieldItems(), record.getFieldItems());
                }).toList();
        try {
            if (filteredFields.size() > 0) {
                jiraFieldService.batchUpsert(context.getTenantId(), filteredFields);
                log.info("Inserted {} jira fields into the database", filteredFields.size());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            customFieldService.insertPopularJiraFieldsToIntegrationConfig(filteredFields, context.getTenantId(), context.getIntegrationId());
        } catch(Exception e) {
            log.error("Unable to auto insert popular jira fields to integration mapping.", e);
        }
    }

    @Override
    public String getDataTypeName() {
        return "fields";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return true;
    }
}
