package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangeSetTest {

    @Test
    public void test() throws JsonProcessingException {
        ChangeSet changeSet = DefaultObjectMapper.get().readValue("{\"createdDate\":\"2022-08-11T07:23:31.177Z\"}", ChangeSet.class);
        assertThat(changeSet.getCreatedDate()).isEqualTo("2022-08-11T07:23:31.177Z");
        assertThat(DateUtils.parseDateTime(changeSet.getCreatedDate())).isEqualTo(Instant.ofEpochMilli(1660202611177L));
    }

}