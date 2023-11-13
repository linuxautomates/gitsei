package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.models.messages.JenkinsPluginJobRunCompleteMessage;
import io.levelops.aggregations.models.messages.JenkinsPluginResultsPreProcessMessage;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class MessagePubService {
    private final String outputBucket;
    private final Publisher jiraPublisher;
    private final Publisher snykPublisher;
    private final Publisher githubPublisher;
    private final Publisher bitbucketPublisher;
    private final Publisher pagerdutyPublisher;
    private final Publisher tenablePublisher;
    private final Publisher zendeskPublisher;
    private final Publisher sonarQubePublisher;
    private final Publisher testRailsPublisher;
    private final Publisher azureDevopsPublisher;
    private final Publisher salesforcePublisher;
    private final ObjectMapper objectMapper;
    private final ProductIntegMappingService mappingService;
    private final Publisher jenkinsPreProcessPluginPublisher;
    private final Publisher jenkinsPluginJobRunCompletePublisher;
    private final Publisher awsDevToolsPublisher;
    private final Publisher gerritPublisher;
    private final Publisher circleCiPublisher;
    private final Publisher droneCiPublisher;
    private final Publisher gitlabPublisher;
    private final Publisher cXSastPublisher;
    private final Publisher blackDuckPublisher;
    private final Publisher coverityPublisher;
    private final Publisher helixPublisher;
    private final Publisher bitbucketServerPublisher;


    @Autowired
    public MessagePubService(@Value("${AGG_OUTPUT_BUCKET:aggregations-levelops}") String outputBucket,
                             @Value("${AGG_PROJECT}") String projectName,
                             @Value("${JIRA_TOPIC}") String jiraTopic,
                             @Value("${SNYK_TOPIC}") String snykTopic,
                             @Value("${GITHUB_TOPIC}") String githubTopic,
                             @Value("${BITBUCKET_TOPIC}") String bitbucketTopic,
                             @Value("${PD_TOPIC}") String pagerdutyTopic,
                             @Value("${TENABLE_TOPIC}") String tenableTopic,
                             @Value("${ZENDESK_TOPIC}") String zendeskTopic,
                             @Value("${BLACKDUCK_TOPIC:default}") String blackDuckTopic,
                             @Value("${COVERITY_TOPIC:default}") String coverityTopic,
                             @Value("${SALESFORCE_TOPIC}") String salesforceTopic,
                             @Value("${SONARQUBE_TOPIC}") String sonarQubeTopic,
                             @Value("${TESTRAILS_TOPIC}") String testRailsTopic,
                             @Value("${AZURE_DEVOPS_TOPIC}") String azureDevopsTopic,
                             @Qualifier("custom") ObjectMapper objectMapper,
                             ProductIntegMappingService mappingService,
                             @Value("${JENKINS_PRE_PROCESS_TOPIC}") String jenkinsPreProcessTopic,
                             @Value("${JENKINS_JOB_RUN_COMPLETE_TOPIC}") String jenkinsJobRunCompleteTopic,
                             @Value("${AWSDEVTOOLS_TOPIC}") String awsDevToolsTopic,
                             @Value("${GERRIT_TOPIC}") String gerritTopic,
                             @Value("${CIRCLECI_TOPIC}") String circleCiTopic,
                             @Value("${DRONECI_TOPIC}") String droneCiTopic,
                             @Value("${GITLAB_TOPIC}") String gitlabTopic,
                             @Value("${CXSAST_TOPIC}") String cxSastTopic,
                             @Value("${HELIX_TOPIC}") String helixTopic,
                             @Value("${BITBUCKET_SERVER_TOPIC}") String bitbucketServerTopic)
            throws IOException {
        this.outputBucket = outputBucket;
        this.objectMapper = objectMapper;
        this.mappingService = mappingService;
        this.droneCiPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, droneCiTopic)).build();
        this.gerritPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, gerritTopic)).build();
        this.circleCiPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, circleCiTopic)).build();
        this.gitlabPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, gitlabTopic)).build();
        this.jiraPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, jiraTopic)).build();
        this.snykPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, snykTopic)).build();
        this.githubPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, githubTopic)).build();
        this.zendeskPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, zendeskTopic)).build();
        this.blackDuckPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, blackDuckTopic)).build();
        this.coverityPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, coverityTopic)).build();
        this.tenablePublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, tenableTopic)).build();
        this.bitbucketPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, bitbucketTopic)).build();
        this.testRailsPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, testRailsTopic)).build();
        this.sonarQubePublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, sonarQubeTopic)).build();
        this.azureDevopsPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, azureDevopsTopic)).build();
        this.pagerdutyPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, pagerdutyTopic)).build();
        this.salesforcePublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, salesforceTopic)).build();
        this.jenkinsPreProcessPluginPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, jenkinsPreProcessTopic)).build();
        this.jenkinsPluginJobRunCompletePublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, jenkinsJobRunCompleteTopic)).build();
        this.awsDevToolsPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, awsDevToolsTopic)).build();
        this.cXSastPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, cxSastTopic)).build();
        this.helixPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, helixTopic)).build();
        this.bitbucketServerPublisher = Publisher.newBuilder(ProjectTopicName.of(projectName, bitbucketServerTopic)).build();
    }

    public void publishIntegAggMessages(String company, Integration integration)
            throws JsonProcessingException, SQLException {
        AppAggMessage.AppAggMessageBuilder msgBuilder = AppAggMessage.builder()
                .integrationType(integration.getApplication())
                .integrationId(integration.getId())
                .outputBucket(outputBucket)
                .customer(company);
        IntegrationType type = IntegrationType.fromString(integration.getApplication());
        List<String> productIds = mappingService.listByFilter(company, null,
                        integration.getId(), 0, 1000).getRecords()
                .stream().map(ProductIntegMapping::getProductId).collect(Collectors.toList());
        if (type == null || productIds.size() == 0) {
            log.info("No product id found but still continuing for tenant={} integration_id={} type={}", company, integration.getId(), integration.getApplication());
        }
        Publisher pub = null;
        switch (type) {
            case JIRA:
                pub = jiraPublisher;
                break;
            case SNYK:
                pub = snykPublisher;
                break;
            case GITHUB:
                pub = githubPublisher;
                break;
            case BITBUCKET:
                pub = bitbucketPublisher;
                break;
            case PAGERDUTY:
                pub = pagerdutyPublisher;
                break;
            case TENABLE:
                pub = tenablePublisher;
                break;
            case ZENDESK:
                pub = zendeskPublisher;
                break;
            case SALESFORCE:
                pub = salesforcePublisher;
                break;
            case SONARQUBE:
                pub = sonarQubePublisher;
                break;
            case TESTRAILS:
                pub = testRailsPublisher;
                break;
            case AZURE_DEVOPS:
                pub = azureDevopsPublisher;
                break;
            case AWSDEVTOOLS:
                pub = awsDevToolsPublisher;
                break;
            case HELIX:
                pub = helixPublisher;
                break;
            case GERRIT:
                pub = gerritPublisher;
                break;
            case CIRCLECI:
                pub = circleCiPublisher;
                break;
            case GITLAB:
                pub = gitlabPublisher;
                break;
            case DRONECI:
                pub = droneCiPublisher;
                break;
            case CXSAST:
                pub = cXSastPublisher;
                break;
            case BITBUCKET_SERVER:
                pub = bitbucketServerPublisher;
                break;
            case BLACKDUCK:
                pub = blackDuckPublisher;
                break;
            case COVERITY:
                pub = coverityPublisher;
                break;
            default:
                //do nothing for now
                return;
        }

        // The use of product id here is deprecated. We will soon remove this from AppAggMessage altogether.
        AppAggMessage message = msgBuilder.productId(productIds.stream().findFirst().orElse(null)).build();
        log.info("Publishing aggregation message for tenant={} integration_id={} type={}: messageId={}", company, integration.getId(), integration.getApplication(), message.getMessageId());
        ApiFuture<String> messageIdFuture = pub.publish(PubsubMessage
                .newBuilder()
                .setData(ByteString.copyFromUtf8(
                        objectMapper.writeValueAsString(message)))
                .build());
        ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback<>() {
            public void onSuccess(String pubSubMessageId) {
                log.info("Published aggregation message for tenant={} integration_id={} type={}: messageId={}, pubSubMessageId={}", company, integration.getId(), integration.getApplication(), message.getMessageId(), pubSubMessageId);
            }

            public void onFailure(Throwable t) {
                log.error("Failed to publish aggregation message for tenant={} integration_id={} type={} messageId={}", company, integration.getId(), integration.getApplication(), message.getMessageId(), t);
            }
        }, MoreExecutors.directExecutor());
    }

    public void publishJenkinsPreProcessMessage(JenkinsPluginResultsPreProcessMessage pubSubMessage) throws JsonProcessingException {
        jenkinsPreProcessPluginPublisher.publish(
                PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(pubSubMessage)))
                        .build()
        );
    }

    public void publishJenkinsPluginJobRunCompleteMessage(JenkinsPluginJobRunCompleteMessage pubSubMessage) throws JsonProcessingException {
        jenkinsPluginJobRunCompletePublisher.publish(
                PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(pubSubMessage)))
                        .build()
        );
    }
}
