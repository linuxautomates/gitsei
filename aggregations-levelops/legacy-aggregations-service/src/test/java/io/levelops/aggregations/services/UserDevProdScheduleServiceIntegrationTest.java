package io.levelops.aggregations.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;
import org.springframework.cloud.gcp.pubsub.core.PubSubDeliveryException;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class UserDevProdScheduleServiceIntegrationTest {
    //@Test
    public void sendMsgToPubSub() throws IOException, InterruptedException {
        Publisher publisher = Publisher.newBuilder(ProjectTopicName.of("levelops-dev", "dev-jenkins-job-run-complete")).build();

        InputStream is = ResourceUtils.getResourceAsStream("adhoc/msgs_1.txt");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                System.out.println(line);
                System.out.println("sending user dev prod request message starting " + line);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(line)).build();
                publisher.publish(pubsubMessage);
                ApiFuture<String> publishFuture = publisher.publish(pubsubMessage);

                final SettableListenableFuture<String> settableFuture = new SettableListenableFuture<>();
                ApiFutures.addCallback(publishFuture, new ApiFutureCallback<String>() {

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println(throwable);
                        PubSubDeliveryException pubSubDeliveryException = new PubSubDeliveryException(pubsubMessage, "errorMessage", throwable);
                        settableFuture.setException(pubSubDeliveryException);
                    }

                    @Override
                    public void onSuccess(String result) {
                        System.out.println("success_done");
                        settableFuture.set(result);
                    }

                });
            }
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

}