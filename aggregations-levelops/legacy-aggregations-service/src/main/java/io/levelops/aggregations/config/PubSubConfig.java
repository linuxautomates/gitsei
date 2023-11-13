package io.levelops.aggregations.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import io.levelops.aggregations.controllers.AggregationsController;
import io.levelops.aggregations.models.messages.AggregationMessage;
import io.levelops.ingestion.models.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Log4j2
@Configuration
public class PubSubConfig {


    // PubSub Stuff
    @Bean("globalInputChannel")
    public MessageChannel globalInputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ConditionalOnExpression("${RULES_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter triageRuleMessageChannelAdapter(
            @Value("${TRIAGE_RULES_SUB}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${OKTA_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter oktaMessageChannelAdapter(
            @Value("${OKTA_AGG_SUB:dev-okta-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${BLACKDUCK_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter blackDuckMessageChannelAdapter(
            @Value("${BLACKDUCK_AGG_SUB:dev-blackduck-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${BITBUCKET_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter bitBucketMessageChannelAdapter(
            @Value("${BITBUCKET_AGG_SUB:dev-bitbucket-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${BITBUCKET_SERVER_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter bitbucketServerMessageChannelAdapter(
            @Value("${BITBUCKET_SERVER_AGG_SUB:dev-bitbucket-server-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${GITHUB_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter githubMessageChannelAdapter(
            @Value("${GITHUB_AGG_SUB:dev-github-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${GERRIT_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter gerritMessageChannelAdapter(
            @Value("${GERRIT_AGG_SUB:dev-gerrit-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${JIRA_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter jiraMessageChannelAdapter(
            @Value("${JIRA_AGG_SUB:dev-jira-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${PAGERDUTY_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter pagerDutyMessageChannelAdapter(
            @Value("${PAGERDUTY_AGG_SUB:dev-pagerduty-agg-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${SNYK_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter snykMessageChannelAdapter(
            @Value("${SNYK_AGG_SUB:dev-snyk-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${TENABLE_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter tenableMessageChannelAdapter(
            @Value("${TENABLE_AGG_SUB:dev-tenable-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${SALESFORCE_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter salesforceMessageChannelAdapter(
            @Value("${SALESFORCE_AGG_SUB:dev-salesforce-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${JENKINS_PRE_PROCESS_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter jenkinsPreProcessMessageChannelAdapter(
            @Value("${JENKINS_PRE_PROCESS_SUB}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${ZENDESK_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter zendeskMessageChannelAdapter(
            @Value("${ZENDESK_AGG_SUB:dev-zendesk-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${SONARQUBE_MAX_THREADS} > 0")
    PubSubInboundChannelAdapter sonarQubeMessageChannelAdapter(
            @Value("${SONARQUBE_AGG_SUB:dev-sonarqube-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${AZURE_DEVOPS_MAX_THREADS} > 0")
    PubSubInboundChannelAdapter azureDevopsMessageChannelAdapter(
            @Value("${AZURE_DEVOPS_AGG_SUB:dev-azure-devops-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${COVERITY_MAX_THREADS} > 0")
    PubSubInboundChannelAdapter coverityMessageChannelAdapter(
            @Value("${COVERITY_AGG_SUB:dev-coverity-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${CIRCLECI_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter circleCIMessageChannelAdapter(
            @Value("${CIRCLECI_AGG_SUB:dev-circleci-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${DRONECI_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter droneCIMessageChannelAdapter(
            @Value("${DRONECI_AGG_SUB:dev-droneci-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${AWSDEVTOOLS_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter aWSDevToolsMessageChannelAdapter(
            @Value("${AWSDEVTOOLS_AGG_SUB:dev-awsdevtools-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }


    @Bean
    @ConditionalOnExpression("${GITLAB_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter gitlabMessageChannelAdapter(
            @Value("${GITLAB_AGG_SUB:dev-gitlab-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }


    @Bean
    @ConditionalOnExpression("${HELIX_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter helixMessageChannelAdapter(
            @Value("${HELIX_AGG_SUB:dev-helix-agg-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ConditionalOnExpression("${CXSAST_MAX_THREADS} > 0")
    public PubSubInboundChannelAdapter cxsastMessageChannelAdapter(
            @Value("${CXSAST_AGG_SUB:dev-checkmarx-sub}") String subscriptionName,
            @Qualifier("globalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "globalInputChannel")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public MessageHandler messageReceiver(
            @Qualifier("aggregationControllers") Map<String, AggregationsController> controllers,
            ObjectMapper mapper) {
        log.info("Controllers registry: {}", controllers);
        return message -> {
            boolean ack = true;
            var messageBytes = (byte[]) message.getPayload();
            var messageText = new String(messageBytes);
            String truncatedMessageText = StringUtils.truncate(messageText, 1024);
            log.info("Message arrived! Payload: {}", truncatedMessageText);
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            try {
                var origin = originalMessage.getProjectSubscriptionName().getSubscription();
                var baseMessage = mapper.readValue(messageText, BaseMessage.class);
                AggregationsController<AggregationMessage> controller = controllers.get(origin);
                Class<? extends AggregationMessage> clazz = controller.getMessageType();
                AggregationMessage aggMessage = mapper.readValue(messageText, clazz);
                log.debug("Origin: {}, Integration type: {}, Controller: {}", origin, baseMessage.getIntegrationType(), controller);

                log.info("Processing: {}", aggMessage);
                controller.doTask(aggMessage);
            } catch (RejectedExecutionException e) {
                log.debug("Could not process message, threads are busy: {} - caused by: {}", truncatedMessageText, e.getMessage());
                ack = false;
            } catch (IOException e) {
                log.error("Error processing the message: {}", truncatedMessageText, e);
            } finally {
                if (ack) {
                    originalMessage.ack();
                } else {
                    originalMessage.nack();
                }
            }
        };
    }

    @Data
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(builder = BaseMessage.BaseMessageBuilder.class)
    public static class BaseMessage implements AggregationMessage {
        @JsonProperty("message_id")
        private String messageId;
        @JsonProperty("integration_type")
        private IntegrationType integrationType;
        @JsonProperty("output_bucket")
        private String outputBucket;
        @JsonProperty("customer")
        private String customer;

    }

    @Bean
    public Publisher scmCommitPRMappingPublisher(@Value("${GOOGLE_CLOUD_PROJECT}") String project, @Value("${SCM_COMMIT_PR_MAPPING_TOPIC}") String topic) throws IOException {
        return Publisher.newBuilder(ProjectTopicName.of(project, topic)).build();
    }

    @Bean("cicdArtifactMappingPublisher")
    public Publisher cicdArtifactMappingPublisher(@Value("${GOOGLE_CLOUD_PROJECT}") String project,
                                                  @Value("${CICD_ARTIFACT_MAPPING_TOPIC}") String topic) throws IOException {
        return Publisher.newBuilder(ProjectTopicName.of(project, topic)).build();
    }
}