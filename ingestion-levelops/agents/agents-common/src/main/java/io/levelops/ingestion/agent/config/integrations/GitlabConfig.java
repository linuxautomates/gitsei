package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.agent.ingestion.GitlabControllers;
import io.levelops.ingestion.agent.ingestion.GitlabIterativeScanController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.sources.GitlabGroupDataSource;
import io.levelops.integrations.gitlab.sources.GitlabIssueDataSource;
import io.levelops.integrations.gitlab.sources.GitlabProjectDataSource;
import io.levelops.integrations.gitlab.sources.GitlabProjectPipelineDataSource;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import io.levelops.integrations.gitlab.sources.GitlabUsersDataSource;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Configuration
public class GitlabConfig {

    public static final String GITLAB_USER_CONTROLLER = "GitlabUserController";
    private static final String GITLAB_ITERATIVE_SCAN_CONTROLLER = "GitlabIterativeScanController";
    private static final String GITLAB_PROJECT_CONTROLLER = "GitlabProjectController";
    private static final String GITLAB_COMMIT_CONTROLLER = "GitlabCommitController";
    private static final String GITLAB_TAG_CONTROLLER = "GitlabTagController";
    private static final String GITLAB_MERGE_REQUEST_CONTROLLER = "GitlabMergeRequestController";
    private static final String GITLAB_MILESTONE_CONTROLLER = "GitlabMilestoneController";

    @Value("${gitlab.onboarding_in_days:7}")
    private Integer gitlabOnboardingInDays;

    @Bean
    public GitlabClientFactory gitlabClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                   OkHttpClient okHttpClient, @Qualifier("allowUnsafeSSLGitlab") Boolean allowUnsafeSSL) {
        return GitlabClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .allowUnsafeSSL(allowUnsafeSSL)
                .build();
    }

    @Bean
    public GitlabIterativeScanController gitlabIterativeScanController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            @Qualifier("gitlabProjectController") IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabProjectController,
            @Qualifier("gitlabCommitController") IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabCommitController,
            @Qualifier("gitlabTagController") IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabTagController,
            @Qualifier("gitlabMergeRequestController") IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabMergeRequestController,
            @Qualifier("gitlabMilestoneController") IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabMilestoneController,
            @Qualifier("gitlabUserController") IntegrationController<GitlabUsersDataSource.GitlabUserQuery> gitlabUserController,
            @Qualifier("gitlabGroupController") IntegrationController<GitlabQuery> gitlabGroupController,
            @Qualifier("gitlabIssueController") IntegrationController<GitlabQuery> gitlabIssueController,
            @Qualifier("gitlabProjectPipelineController") IntegrationController<GitlabProjectPipelineDataSource.GitlabProjectQuery> gitlabProjectPipelineController,
            InventoryService inventoryService) {

        return ingestionEngine.add(GITLAB_ITERATIVE_SCAN_CONTROLLER, GitlabIterativeScanController.builder()
                .objectMapper(objectMapper)
                .projectController(gitlabProjectController)
                .commitController(gitlabCommitController)
                .tagController(gitlabTagController)
                .mergeRequestController(gitlabMergeRequestController)
                .userController(gitlabUserController)
                .milestoneController(gitlabMilestoneController)
                .groupController(gitlabGroupController)
                .issueController(gitlabIssueController)
                .projectPipelineController(gitlabProjectPipelineController)
                .onboardingInDays(gitlabOnboardingInDays)
                .inventoryService(inventoryService)
                .build());
    }

    @Bean("gitlabProjectController")
    public IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabProjectController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabProjectDataSource projectDataSource = ingestionEngine.add("GitlabProjectDataSource",
                new GitlabProjectDataSource(gitlabClientFactory));

        return ingestionEngine.add(GITLAB_PROJECT_CONTROLLER, GitlabControllers.projectController()
                .objectMapper(objectMapper)
                .dataSource(projectDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabCommitController")
    public IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabCommitController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabProjectDataSource gitlabCommitDataSource = ingestionEngine.add("GitlabCommitDataSource",
                new GitlabProjectDataSource(gitlabClientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.COMMITS)));

        return ingestionEngine.add(GITLAB_COMMIT_CONTROLLER, GitlabControllers.commitController()
                .objectMapper(objectMapper)
                .dataSource(gitlabCommitDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabTagController")
    public IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabTagController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabProjectDataSource gitlabTagDataSource = ingestionEngine.add("GitlabTagDataSource",
                new GitlabProjectDataSource(gitlabClientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.TAGS)));

        return ingestionEngine.add(GITLAB_TAG_CONTROLLER, GitlabControllers.tagController()
                .objectMapper(objectMapper)
                .dataSource(gitlabTagDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabMergeRequestController")
    public IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabMergeRequestController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabProjectDataSource gitlabMergeRequestDataSource = ingestionEngine.add("GitlabMergeRequestDataSource",
                new GitlabProjectDataSource(gitlabClientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.MERGE_REQUESTS)));

        return ingestionEngine.add(GITLAB_MERGE_REQUEST_CONTROLLER, GitlabControllers.mergeRequestController()
                .objectMapper(objectMapper)
                .dataSource(gitlabMergeRequestDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabMilestoneController")
    public IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> gitlabMilestoneController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabProjectDataSource gitlabMilestoneDataSource = ingestionEngine.add("GitlabMilestoneDataSource",
                new GitlabProjectDataSource(gitlabClientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.MILESTONES)));

        return ingestionEngine.add(GITLAB_MILESTONE_CONTROLLER, GitlabControllers.milestonesController()
                .objectMapper(objectMapper)
                .dataSource(gitlabMilestoneDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabUserController")
    public IntegrationController<GitlabUsersDataSource.GitlabUserQuery> gitlabUserController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabUsersDataSource gitlabUserDataSource = ingestionEngine.add("GitlabUserDataSource",
                new GitlabUsersDataSource(gitlabClientFactory));

        return ingestionEngine.add(GITLAB_USER_CONTROLLER, GitlabControllers.usersController()
                .objectMapper(objectMapper)
                .dataSource(gitlabUserDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabGroupController")
    public IntegrationController<GitlabQuery> gitlabGroupController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabGroupDataSource gitlabGroupDataSource = ingestionEngine.add("GitlabGroupDataSource",
                new GitlabGroupDataSource(gitlabClientFactory));

        return ingestionEngine.add("GitlabGroupController", GitlabControllers.groupController()
                .objectMapper(objectMapper)
                .dataSource(gitlabGroupDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }



    @Bean("gitlabProjectPipelineController")
    public IntegrationController<GitlabProjectPipelineDataSource.GitlabProjectQuery> gitlabPipelineController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabProjectPipelineDataSource gitlabPipelineDataSource = ingestionEngine.add("GitlabProjectPipelineDataSource",
                new GitlabProjectPipelineDataSource(gitlabClientFactory));

        return ingestionEngine.add("GitlabProjectPipelineController", GitlabControllers.projectPipelineController()
                .objectMapper(objectMapper)
                .dataSource(gitlabPipelineDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }

    @Bean("gitlabIssueController")
    public IntegrationController<GitlabQuery> gitlabIssueController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            GitlabClientFactory gitlabClientFactory) {

        GitlabIssueDataSource gitlabIssueDataSource = ingestionEngine.add("GitlabIssueDataSource",
                new GitlabIssueDataSource(gitlabClientFactory, EnumSet.of(GitlabIssueDataSource.Enrichment.NOTES)));

        return ingestionEngine.add("GitlabIssueController", GitlabControllers.issueController()
                .objectMapper(objectMapper)
                .dataSource(gitlabIssueDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }
}
