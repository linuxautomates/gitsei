package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import io.levelops.aggregations.controllers.AckAggregationsController;
import io.levelops.aggregations.models.messages.AggregationMessage;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Log4j2
@Data
public class AsyncMessageReceiver {
    private final String subscriptionName;
    private MessageReceiver asyncMessageReceiver;

    AsyncMessageReceiver(String subscriptionName, Map<String, AckAggregationsController> controllers, ObjectMapper mapper) {
        this.subscriptionName = subscriptionName;
        this.asyncMessageReceiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            /*
            shouldAckOrNack == null -> neither ack or nack
            shouldAckOrNack == true -> do ack
            shouldAckOrNack == false -> do nack
             */
            Boolean shouldAckOrNack = null;
            // Handle incoming message, then ack the received message.
            String messageText = null;
            try {
                String messageId = message.getMessageId();
                messageText = message.getData().toStringUtf8();
                log.info("Message arrived! Payload: {}", StringUtils.truncate(messageText, 1024));

                AckAggregationsController<AggregationMessage> controller = controllers.get(subscriptionName);
                log.info("Controller: {}", controller);
                Class<? extends AggregationMessage> clazz = controller.getMessageType();
                AggregationMessage essage = mapper.readValue(messageText, clazz);
                log.info("Processing: {}", essage);
                controller.doTask(essage, consumer);
            } catch (RejectedExecutionException e) {
                log.debug("Could not process message, threads are busy: {} - caused by: {}", messageText, e.getMessage());
                shouldAckOrNack = false;
            } catch (IOException e) {
                log.error("Error processing the message: {}", messageText, e);
                shouldAckOrNack = true;
            } finally {
                if (BooleanUtils.isTrue(shouldAckOrNack)) {
                    consumer.ack();
                } else if (BooleanUtils.isFalse(shouldAckOrNack)) {
                    consumer.nack();
                }
            }
        };
    }

    public static AsyncMessageReceiverBuilder builder() {
        return new AsyncMessageReceiverBuilder();
    }

    public static class AsyncMessageReceiverBuilder {
        private String subscriptionName;
        private Map<String, AckAggregationsController> controllers;
        private ObjectMapper mapper;

        AsyncMessageReceiverBuilder() {
        }

        public AsyncMessageReceiverBuilder subscriptionName(String subscriptionName) {
            this.subscriptionName = subscriptionName;
            return this;
        }

        public AsyncMessageReceiverBuilder controllers(Map<String, AckAggregationsController> controllers) {
            this.controllers = controllers;
            return this;
        }

        public AsyncMessageReceiverBuilder mapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public AsyncMessageReceiver build() {
            return new AsyncMessageReceiver(subscriptionName, controllers, mapper);
        }

        public String toString() {
            return "AsyncMessageReceiver.AsyncMessageReceiverBuilder(subscriptionName=" + this.subscriptionName + ", controllers=" + this.controllers + ", mapper=" + this.mapper + ")";
        }
    }
}
