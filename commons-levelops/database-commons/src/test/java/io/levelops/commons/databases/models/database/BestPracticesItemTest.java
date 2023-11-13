package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class BestPracticesItemTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws JsonProcessingException {
        Long time = System.currentTimeMillis();
        BestPracticesItem expected = BestPracticesItem.builder()
                .id(UUID.randomUUID())
                .name("name")
                .type(BestPracticesItem.BestPracticeType.FILE)
                .value("value")
                .createdAt(time).updatedAt(time)
                .tags(List.of("tag1", "tag2"))
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        BestPracticesItem actual = MAPPER.readValue(serialized, BestPracticesItem.class);
        Assert.assertEquals(expected, actual);

        expected = expected.toBuilder().metadata("filename.txt").build();
        serialized = MAPPER.writeValueAsString(expected);
        actual = MAPPER.readValue(serialized, BestPracticesItem.class);
        Assert.assertEquals(expected, actual);
    }
}