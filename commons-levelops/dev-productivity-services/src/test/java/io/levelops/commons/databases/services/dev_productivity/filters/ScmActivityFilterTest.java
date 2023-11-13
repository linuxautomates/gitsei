package io.levelops.commons.databases.services.dev_productivity.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ScmActivityFilterTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromListRequest() throws JsonProcessingException {
        String data = "{\n" +
                "    \"page\": 0,\n" +
                "    \"page_size\": 100,\n" +
                "    \"filter\": {\n" +
                "        \"across\" : \"integration_user\",\n" +
                "        \"user_id_type\": \"ou_user_ids\",\n" +
                "        \"user_id\": \"fa967188-7e8f-4184-9683-8b3a60ec1b85\",\n" +
                "        \"time_range\": {\n" +
                "            \"$gt\": \"1633564800\",\n" +
                "            \"$lt\": \"1633996799\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        DefaultListRequest filter = MAPPER.readValue(data, DefaultListRequest.class);
        ScmActivityFilter scmActivityFilter = ScmActivityFilter.fromListRequest(filter);
        Assert.assertNotNull(scmActivityFilter);
        Assert.assertEquals(ScmActivityFilter.DISTINCT.integration_user, scmActivityFilter.getAcross());
        Assert.assertEquals(1633564800l, scmActivityFilter.getTimeRange().getLeft().longValue());
        Assert.assertEquals(1633996799l, scmActivityFilter.getTimeRange().getRight().longValue());

        Assert.assertEquals(IdType.OU_USER_IDS, scmActivityFilter.getUserIdType());
        Assert.assertEquals(UUID.fromString("fa967188-7e8f-4184-9683-8b3a60ec1b85"), scmActivityFilter.getUserId());
    }
}