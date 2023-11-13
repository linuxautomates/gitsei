package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.NumberedPaginationStrategy;
import io.levelops.ingestion.strategies.pagination.SinglePageStrategy;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraSprint;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraUserEmail;
import io.levelops.integrations.jira.sources.JiraIssueDataSource;
import io.levelops.integrations.jira.sources.JiraIssueDataSource.JiraIssueQuery;
import io.levelops.integrations.jira.sources.JiraProjectDataSource;
import io.levelops.integrations.jira.sources.JiraProjectDataSource.JiraProjectQuery;
import io.levelops.integrations.jira.sources.JiraSprintDataSource;
import io.levelops.integrations.jira.sources.JiraSprintDataSource.JiraSprintQuery;
import io.levelops.integrations.jira.sources.JiraUserEmailsDataSource;
import lombok.Builder;

import javax.annotation.Nullable;

import static io.levelops.integrations.jira.sources.JiraUserEmailsDataSource.*;

public class JiraControllers {

    private static final Integer PAGE_SIZE = 50;

    @Builder(builderMethodName = "issueController", builderClassName = "JiraIssueControllerBuilder")
    private static IntegrationController<JiraIssueQuery> buildJiraIssueController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraIssueDataSource issueDataSource,
            @Nullable Integer outputPageSize) {

        return PaginatedIntegrationController.<JiraIssue, JiraIssueQuery>builder()
                .queryClass(JiraIssueQuery.class)
                .paginationStrategy(NumberedPaginationStrategy.<JiraIssue, JiraIssueQuery>builder()
                        .objectMapper(objectMapper)
                        .storageDataSink(storageDataSink)
                        .integrationType("jira")
                        .dataType("issues")
                        .pageDataSupplier((query, page) -> {
                            int pageSize = MoreObjects.firstNonNull(query.getLimit(), PAGE_SIZE);
                            return issueDataSource.fetchMany(JiraIssueQuery.builder()
                                    .integrationKey(query.getIntegrationKey())
                                    .jql(query.getJql())
                                    .skip(page.getPageNumber() * pageSize)
                                    .limit(pageSize)
                                    .build());
                        })
                        .outputPageSize(outputPageSize)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "projectController", builderClassName = "JiraProjectControllerBuilder")
    private static IntegrationController<JiraProjectQuery> buildJiraProjectController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraProjectDataSource projectDataSource) {

        return PaginatedIntegrationController.<JiraProject, JiraProjectQuery>builder()
                .queryClass(JiraProjectQuery.class)
                .paginationStrategy(SinglePageStrategy.<JiraProject, JiraProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .storageDataSink(storageDataSink)
                        .dataSource(projectDataSource)
                        .integrationType("jira")
                        .dataType("projects")
                        .build())
                .build();
    }

    @Builder(builderMethodName = "sprintController", builderClassName = "JiraSprintControllerBuilder")
    private static IntegrationController<JiraSprintQuery> buildJiraSprintController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraSprintDataSource sprintDataSource) {

        return PaginatedIntegrationController.<JiraSprint, JiraSprintQuery>builder()
                .queryClass(JiraSprintQuery.class)
                .paginationStrategy(SinglePageStrategy.<JiraSprint, JiraSprintQuery>builder()
                        .objectMapper(objectMapper)
                        .storageDataSink(storageDataSink)
                        .dataSource(sprintDataSource)
                        .integrationType("jira")
                        .dataType("sprints")
                        .build())
                .build();
    }

    @Builder(builderMethodName = "userEmailsController", builderClassName = "JiraUserEmailsControllerBuilder")
    private static IntegrationController<JiraUserEmailQuery> buildUserEmailsController(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraUserEmailsDataSource userEmailsDataSource) {
        return PaginatedIntegrationController.<JiraUserEmail, JiraUserEmailQuery>builder()
                .queryClass(JiraUserEmailQuery.class)
                .paginationStrategy(SinglePageStrategy.<JiraUserEmail, JiraUserEmailQuery>builder()
                        .objectMapper(objectMapper)
                        .storageDataSink(storageDataSink)
                        .dataSource(userEmailsDataSource)
                        .integrationType("jira")
                        .dataType("user_emails")
                        .build())
                .build();
    }

}
