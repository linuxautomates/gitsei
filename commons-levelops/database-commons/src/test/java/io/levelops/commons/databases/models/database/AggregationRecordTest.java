package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class AggregationRecordTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void test() throws IOException {
        String serializedRecords = ResourceUtils.getResourceAsString("aggregation_record/aggregation_records.json");
        PaginatedResponse<AggregationRecord> result = MAPPER.readValue(serializedRecords, MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, AggregationRecord.class));
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getResponse().getRecords().size());
    }
}