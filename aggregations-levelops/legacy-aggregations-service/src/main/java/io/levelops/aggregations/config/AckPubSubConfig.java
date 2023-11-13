package io.levelops.aggregations.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.pubsub.v1.ProjectSubscriptionName;
import io.levelops.aggregations.controllers.AckAggregationsController;
import io.levelops.aggregations.services.AsyncMessageReceiver;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.bp.Duration;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Configuration
public class AckPubSubConfig {

    private static final int DEFAULT_QUEUE_SIZE = 50;

    @Bean
    public List<SubscriberConfig> ackSubscriptionConfigs(
            @Value("${SCM_COMMIT_PR_MAPPING_SUB}") String scmCommitPRMappingScheduleSubName,
            @Value("${SCM_COMMIT_PR_MAPPING_MAX_THREADS:5}") final Integer scmCommitPRMappingMaxPoolSize,
            @Value("${SCM_COMMIT_PR_MAPPING_QUEUE_SIZE:5}") final Integer scmCommitPRMappingQueueSize,
            @Value("${JENKINS_JOB_RUN_COMPLETE_SUB}") String jenkinsJobRunCompleteSubName,
            @Value("${JENKINS_JOB_RUN_COMPLETE_MAX_THREADS:2}") final Integer jenkinsJobRunCompleteMaxPoolSize,
            @Value("${JENKINS_JOB_RUN_COMPLETE_QUEUE_SIZE:50}") final Integer jenkinsJobRunCompleteQueueSize,
            @Value("${CICD_ARTIFACT_MAPPING_SUB}") String cicdArtifactMappingSub,
            @Value("${CICD_ARTIFACT_MAPPING_MAX_THREADS:5}") final Integer cicdArtifactMappingMaxPoolSize,
            @Value("${CICD_ARTIFACT_MAPPING_QUEUE_SIZE:5}") final Integer cicdArtifactMappingQueueSize
    ) {
        List<SubscriberConfig> list = Lists.newArrayList();
        addSubscriberConfig(list, scmCommitPRMappingScheduleSubName, scmCommitPRMappingMaxPoolSize, scmCommitPRMappingQueueSize);
        addSubscriberConfig(list, jenkinsJobRunCompleteSubName, jenkinsJobRunCompleteMaxPoolSize, jenkinsJobRunCompleteQueueSize);
        addSubscriberConfig(list, cicdArtifactMappingSub, cicdArtifactMappingMaxPoolSize, cicdArtifactMappingQueueSize);
        return list;
    }

    private static void addSubscriberConfig(List<SubscriberConfig> list,
                                            String subName,
                                            Integer poolSize,
                                            Integer queueSize) {
        if (poolSize == null || poolSize <= 0) {
            return;
        }
        Validate.notBlank(subName, "subName cannot be null or empty.");
        list.add(SubscriberConfig.builder()
                .subName(subName)
                .maxPoolSize(poolSize)
                .queueSize(MoreObjects.firstNonNull(queueSize, DEFAULT_QUEUE_SIZE))
                .build());
    }

    @Bean
    @ConditionalOnExpression("" +
            " ${SCM_COMMIT_PR_MAPPING_MAX_THREADS} > 0 or " +
            " ${JENKINS_JOB_RUN_COMPLETE_MAX_THREADS} > 0 or " +
            " ${CICD_ARTIFACT_MAPPING_MAX_THREADS} > 0 ")
    public List<Subscriber> ackSubscriptions(@Value("${GOOGLE_CLOUD_PROJECT}") String project,
                                             List<SubscriberConfig> ackSubscriptionConfigs,
                                             @Qualifier("ackAggregationControllers") Map<String, AckAggregationsController> controllers, ObjectMapper mapper) {
        List<Subscriber> subscribers = new ArrayList<>();
        for (SubscriberConfig s : ackSubscriptionConfigs) {
            log.info("SubscriberConfig = {}", s);
            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(project, s.getSubName());
            // The subscriber will pause the message stream and stop receiving more messsages from the
            // server if any one of the conditions is met.
            FlowControlSettings flowControlSettings =
                    FlowControlSettings.newBuilder()
                            // 1,000 outstanding messages. Must be >0. It controls the maximum number of messages
                            // the subscriber receives before pausing the message stream.
                            .setMaxOutstandingElementCount(s.getMaxPoolSize().longValue())
                            .build();
            Subscriber subscriber =
                    Subscriber.newBuilder(subscriptionName, AsyncMessageReceiver.builder().subscriptionName(s.getSubName()).mapper(mapper).controllers(controllers).build().getAsyncMessageReceiver())
                            .setMaxAckExtensionPeriod(Duration.of(1L, ChronoUnit.HOURS))
                            .setFlowControlSettings(flowControlSettings)
                            .build();
            // Start the subscriber.
            subscriber.startAsync().awaitRunning();
            log.info("Listening for messages on {}", subscriptionName);
            subscribers.add(subscriber);
        }
        return subscribers;
    }


    @Builder(toBuilder = true)
    @Data
    public static class SubscriberConfig {
        private final String subName;
        private final Integer maxPoolSize;
        private final Integer queueSize;
    }
}
