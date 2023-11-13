package io.levelops.commons.databases.models.database;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class EventTest {
    //Default Start Time
    private static final Long LAST_ONE_DAY = Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli(); // last 24 hours for the time being
    private static final Long LAST_SEVEN_DAYS = Instant.now().minus(7l, ChronoUnit.DAYS).toEpochMilli(); // last 7 days
    @Test
    public void serializationTest() throws IOException {
        var expected = Event.builder()
            .company("company")
            .type(EventType.PRAETORIAN_REPORT_CREATED)
            .build();
        var text = DefaultObjectMapper.get().writeValueAsString(expected);
        Assertions.assertThat(text).isNotNull();

        String serialized = DefaultObjectMapper.get().writeValueAsString(expected);
        Assertions.assertThat(serialized).isNotNull();
        Event actual = DefaultObjectMapper.get().readValue(serialized, Event.class);

        //Check if start_time is specified in the event, use default if not specified in the event
        Long start = (Long) MapUtils.emptyIfNull(actual.getData()).getOrDefault("start_time", LAST_ONE_DAY);
        Assert.assertEquals(LAST_ONE_DAY, start);
    }

    @Test
    public void serializationTest2() throws IOException {
        var expected = Event.builder()
                .company("jci")
                .type(EventType.BITBUCKET_NEW_AGGREGATION)
                .data(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "start_time", LAST_SEVEN_DAYS
                ))
                .build();
        var text = DefaultObjectMapper.get().writeValueAsString(expected);
        Assertions.assertThat(text).isNotNull();
        String serialized = DefaultObjectMapper.get().writeValueAsString(expected);
        Assertions.assertThat(serialized).isNotNull();
        Event actual = DefaultObjectMapper.get().readValue(serialized, Event.class);

        //Check if start_time is specified in the event, use default if not specified in the event
        Long start = (Long) MapUtils.emptyIfNull(actual.getData()).getOrDefault("start_time", LAST_ONE_DAY);
        Assert.assertEquals(LAST_SEVEN_DAYS, start);
    }


    @Test
    public void test() throws IOException {
        var event = ResourceUtils.getResourceAsObject("samples/database/event.json", Event.class);
        Assertions.assertThat(event).isNotNull();
        Assertions.assertThat(event.getCompany()).isEqualTo("foo");
        Assertions.assertThat(event.getType()).isEqualTo(EventType.PRAETORIAN_REPORT_CREATED);
    }
}