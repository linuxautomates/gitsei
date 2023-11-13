package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class DevProductivityUserIdsTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws JsonProcessingException {
        String data = "{\n" +
                "    \"page\": 0,\n" +
                "    \"page_size\": 100,\n" +
                "    \"across\": \"velocity\",\n" +
                "    \"filter\": {\n" +
                "        \"dev_productivity_profile_id\": \"GUID/Optional\",\n" +
                "        \"user_id_type\": \"integration_user_ids\",\n" +
                "        \"user_id_list\": [\n" +
                "            \"35e7aba1-9840-4e8c-919e-eb59f285fe98\",\n" +
                "            \"2cfa48aa-0540-46bb-bcf6-52859014071e\"\n" +
                "        ],\n" +
                "        \"time_range\": {\n" +
                "            \"$gt\": \"1633564800\",\n" +
                "            \"$lt\": \"1633996799\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        DefaultListRequest filter = MAPPER.readValue(data, DefaultListRequest.class);
        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.fromListRequest(filter);
        Assert.assertEquals(IdType.INTEGRATION_USER_IDS, devProductivityUserIds.getUserIdType());
        Assert.assertEquals(List.of(UUID.fromString("35e7aba1-9840-4e8c-919e-eb59f285fe98"), UUID.fromString("2cfa48aa-0540-46bb-bcf6-52859014071e")), devProductivityUserIds.getUserIdList());
    }
}