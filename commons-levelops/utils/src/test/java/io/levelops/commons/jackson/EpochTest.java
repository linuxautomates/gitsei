package io.levelops.commons.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class EpochTest {

    static class TestPojo {
        @JsonProperty("date")
        @JsonDeserialize(using = EpochSecondsToDateDeserializer.class)
        @JsonSerialize(using = DateToEpochSecondsSerializer.class)
        Date date;
    }

    @Test
    public void testDeserialize() throws IOException {
        TestPojo testPojo = DefaultObjectMapper.get().readValue("{ \"date\": 1573168088 }", TestPojo.class);
        assertThat(testPojo.date).isEqualTo("2019-11-07T23:08:08Z");
    }


    @Test
    public void testDeserializeNull() throws IOException {
        TestPojo testPojo = DefaultObjectMapper.get().readValue("{ }", TestPojo.class);
        assertThat(testPojo.date).isNull();
    }

    @Test
    public void testDeserializeInvalid() throws IOException {
        TestPojo testPojo = DefaultObjectMapper.get().readValue("{ \"date\": \"abc\" }", TestPojo.class);
        assertThat(testPojo.date).isNull();
    }

    @Test
    public void testSerialize() throws IOException {
        TestPojo testPojo1 = new TestPojo();
        testPojo1.date = new Date(1573168088000L);
        String out = DefaultObjectMapper.get().writeValueAsString(testPojo1);
        assertThat(out).isEqualTo("{\"date\":1573168088}");
    }

    @Test
    public void testSerializeNull() throws IOException {
        TestPojo testPojo1 = new TestPojo();
        testPojo1.date = null;
        String out = DefaultObjectMapper.get().writeValueAsString(testPojo1);
        assertThat(out).isEqualTo("{}");
    }

    @Test
    public void testConvert() {
        HashMap<Object, Object> map = new HashMap<>();
        map.put("date", 1573168088L);
        TestPojo testPojo = DefaultObjectMapper.get().convertValue(map, TestPojo.class);
        assertThat(testPojo.date).isEqualTo("2019-11-07T23:08:08Z");
    }
}