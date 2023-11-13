package io.levelops.aggregations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadingConfig {
    // Aggregations threads... stuff
    @Bean(name = "oktaTaskExecutor")
    @ConditionalOnExpression("${OKTA_MAX_THREADS} > 0")
    public Executor oktaTaskExecutor(
            @Value("${OKTA_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${OKTA_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("okta-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "bitBucketTaskExecutor")
    @ConditionalOnExpression("${BITBUCKET_MAX_THREADS} > 0")
    public Executor bitBucketTaskExecutor(
            @Value("${BITBUCKET_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${BITBUCKET_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("bitbucket-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "blackDuckTaskExecutor")
    @ConditionalOnExpression("${BLACKDUCK_MAX_THREADS} > 0")
    public Executor blackDuckTaskExecutor(
            @Value("${BLACKDUCK_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${BLACKDUCK_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("blackduck-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "bitbucketServerTaskExecutor")
    @ConditionalOnExpression("${BITBUCKET_SERVER_MAX_THREADS} > 0")
    public Executor bitbucketServerTaskExecutor(
            @Value("${BITBUCKET_SERVER_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${BITBUCKET_SERVER_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("bitbucket_server-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "githubTaskExecutor")
    @ConditionalOnExpression("${GITHUB_MAX_THREADS} > 0")
    public Executor githubTaskExecutor(
            @Value("${GITHUB_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${GITHUB_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("github-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "gerritTaskExecutor")
    @ConditionalOnExpression("${GERRIT_MAX_THREADS} > 0")
    public Executor gerritTaskExecutor(
            @Value("${GERRIT_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${GERRIT_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("gerrit-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jiraTaskExecutor")
    @ConditionalOnExpression("${JIRA_MAX_THREADS} > 0")
    public Executor jiraTaskExecutor(
            @Value("${JIRA_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${JIRA_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("jira-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "pagerDutyTaskExecutor")
    @ConditionalOnExpression("${PAGERDUTY_MAX_THREADS} > 0")
    public Executor pagerDutyTaskExecutor(
            @Value("${PAGERDUTY_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${PAGERDUTY_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("pagerduty-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "snykTaskExecutor")
    @ConditionalOnExpression("${SNYK_MAX_THREADS} > 0")
    public Executor snykTaskExecutor(
            @Value("${SNYK_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${SNYK_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("snyk-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "tenableTaskExecutor")
    @ConditionalOnExpression("${TENABLE_MAX_THREADS} > 0")
    public Executor tenableTaskExecutor(
            @Value("${TENABLE_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${TENABLE_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("tenable-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "salesforceTaskExecutor")
    @ConditionalOnExpression("${SALESFORCE_MAX_THREADS} > 0")
    public Executor salesforceTaskExecutor(
            @Value("${SALESFORCE_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${SALESFORCE_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("salesforce-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jenkinsPluginPreProcessTaskExecutor")
    @ConditionalOnExpression("${JENKINS_PRE_PROCESS_MAX_THREADS} > 0")
    public Executor jenkinsPluginPreProcessTaskExecutor(
            @Value("${JENKINS_PRE_PROCESS_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${JENKINS_PRE_PROCESS_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("jenkins-pre-process-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jenkinsPluginJobRunCompleteTaskExecutor")
    @ConditionalOnExpression("${JENKINS_JOB_RUN_COMPLETE_MAX_THREADS} > 0")
    public Executor jenkinsPluginJobRunCompleteTaskExecutor(
            @Value("${JENKINS_JOB_RUN_COMPLETE_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${JENKINS_JOB_RUN_COMPLETE_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("jenkins-job-run-complete-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "zendeskTaskExecutor")
    @ConditionalOnExpression("${ZENDESK_MAX_THREADS} > 0")
    public Executor zendeskTaskExecutor(
            @Value("${ZENDESK_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${ZENDESK_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("zendesk-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "rulesTaskExecutor")
    @ConditionalOnExpression("${RULES_MAX_THREADS} > 0")
    public Executor rulesTaskExecutor(
            @Value("${RULES_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${RULES_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("rules-");
        executor.initialize();
        return executor;
    }


    @Bean(name = "sonarQubeTaskExecutor")
    @ConditionalOnExpression("${SONARQUBE_MAX_THREADS} > 0")
    public Executor sonarQubeTaskExecutor(
            @Value("${SONARQUBE_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${SONARQUBE_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("sonarqube-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "gitlabTaskExecutor")
    @ConditionalOnExpression("${GITLAB_MAX_THREADS} > 0")
    public Executor gitlabTaskExecutor(
            @Value("${GITLAB_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${GITLAB_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("gitlab-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "droneCITaskExecutor")
    @ConditionalOnExpression("${DRONECI_MAX_THREADS} > 0")
    public Executor droneCITaskExecutor(
            @Value("${DRONECI_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${DRONECI_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("droneci-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "azureDevopsTaskExecutor")
    @ConditionalOnExpression("${AZURE_DEVOPS_MAX_THREADS} > 0")
    public Executor azureDevopsTaskExecutor(
            @Value("${AZURE_DEVOPS_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${AZURE_DEVOPS_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("azure_devops-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "coverityTaskExecutor")
    @ConditionalOnExpression("${COVERITY_MAX_THREADS} > 0")
    public Executor coverityTaskExecutor(
            @Value("${COVERITY_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${COVERITY_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("coverity-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "circleCITaskExecutor")
    @ConditionalOnExpression("${CIRCLECI_MAX_THREADS} > 0")
    public Executor circleCITaskExecutor(
            @Value("${CIRCLECI_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${CIRCLECI_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("circleci-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "aWSDevToolsTaskExecutor")
    @ConditionalOnExpression("${AWSDEVTOOLS_MAX_THREADS} > 0")
    public Executor aWSDevToolsTaskExecutor(
            @Value("${AWSDEVTOOLS_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${AWSDEVTOOLS_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("awsdevtools-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "helixTaskExecutor")
    @ConditionalOnExpression("${HELIX_MAX_THREADS} > 0")
    public Executor helixTaskExecutor(
            @Value("${HELIX_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${HELIX_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("helix-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "cxsastTaskExecutor")
    @ConditionalOnExpression("${CXSAST_MAX_THREADS} > 0")
    public Executor cxsastTaskExecutor(
            @Value("${CXSAST_MAX_THREADS:2}") final Integer maxPoolSize,
            @Value("${CXSAST_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("cxsast-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "scmCommitPRMappingTaskExecutor")
    @ConditionalOnExpression("${SCM_COMMIT_PR_MAPPING_MAX_THREADS} > 0")
    public Executor scmCommitPRMappingTaskExecutor(
            @Value("${SCM_COMMIT_PR_MAPPING_MAX_THREADS:5}") final Integer maxPoolSize,
            @Value("${SCM_COMMIT_PR_MAPPING_QUEUE_SIZE:5}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("scm-commit-pr-mapping-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "cicdArtifactMappingTaskExecutor")
    @ConditionalOnExpression("${CICD_ARTIFACT_MAPPING_MAX_THREADS} > 0")
    public Executor cicdArtifactMappingTaskExecutor(
            @Value("${CICD_ARTIFACT_MAPPING_MAX_THREADS:5}") final Integer maxPoolSize,
            @Value("${CICD_ARTIFACT_MAPPING_QUEUE_SIZE:5}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("cicd-artifact-mapping-");
        executor.initialize();
        return executor;
    }
}
