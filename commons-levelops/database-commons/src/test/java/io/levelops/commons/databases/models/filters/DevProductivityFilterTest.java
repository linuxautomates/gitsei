package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class DevProductivityFilterTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws JsonProcessingException {
        String data = "{\n" +
                "    \"page\": 0,\n" +
                "    \"page_size\": 100,\n" +
                "    \"filter\":\n" +
                "    {\n" +
                "        \"across\": \"repo_id\",\n" +
                "        \"time_range\":\n" +
                "        {\n" +
                "            \"$gt\": \"1633564800\",\n" +
                "            \"$lt\": \"1633996799\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        DefaultListRequest filter = MAPPER.readValue(data, DefaultListRequest.class);
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.fromListRequest(filter);
        Assert.assertNotNull(devProductivityFilter);
        Assert.assertEquals(1633564800l, devProductivityFilter.getTimeRange().getLeft().longValue());
        Assert.assertEquals(1633996799l, devProductivityFilter.getTimeRange().getRight().longValue());

        data = "{\n" +
                "    \"page\": 0,\n" +
                "    \"page_size\": 100,\n" +
                "    \"filter\":\n" +
                "    {\n" +
                "        \"across\": \"repo_id\",\n" +
                "        \"interval\": \"last_month\"\n" +
                "    }\n" +
                "}";
        filter = MAPPER.readValue(data, DefaultListRequest.class);
        devProductivityFilter = DevProductivityFilter.fromListRequest(filter);
        Assert.assertNotNull(devProductivityFilter);
        Assert.assertEquals(ReportIntervalType.LAST_MONTH.getIntervalTimeRange(Instant.now()).getTimeRange(), devProductivityFilter.getTimeRange());

        data = "{\n" +
                "    \"page\": 0,\n" +
                "    \"page_size\": 100,\n" +
                "    \"filter\":\n" +
                "    {\n" +
                "        \"across\": \"repo_id\",\n" +
                "        \"interval\": \"last_month\",\n" +
                "        \"time_range\":\n" +
                "        {\n" +
                "            \"$gt\": \"1633564800\",\n" +
                "            \"$lt\": \"1633996799\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        filter = MAPPER.readValue(data, DefaultListRequest.class);
        devProductivityFilter = DevProductivityFilter.fromListRequest(filter);
        Assert.assertNotNull(devProductivityFilter);
        Assert.assertEquals(1633564800l, devProductivityFilter.getTimeRange().getLeft().longValue());
        Assert.assertEquals(1633996799l, devProductivityFilter.getTimeRange().getRight().longValue());
    }
}