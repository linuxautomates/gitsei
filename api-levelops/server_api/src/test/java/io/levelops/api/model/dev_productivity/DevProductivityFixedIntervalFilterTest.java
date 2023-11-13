package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Assert;
import org.junit.Test;

public class DevProductivityFixedIntervalFilterTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws JsonProcessingException {
        String data = "{\n" +
                "    \"page\": 0,\n" +
                "    \"page_size\": 100,\n" +
                "    \"filter\": {\n" +
                "        \"dev_productivity_profile_id\": \"GUID/Optional\",\n" +
                "        \"interval\" : \"LAST_QUARTER\",\n" +
                "        \"ou_ids\" : [\"1a139a09-b447-4365-a2cb-e5d6cd64e59b\", \"1a139a09-b447-4365-a2cb-e5d6cd64e59c\"]\n" +
                "    }\n" +
                "}";
        DefaultListRequest filter = MAPPER.readValue(data, DefaultListRequest.class);
        DevProductivityFixedIntervalFilter devProdFilter = DevProductivityFixedIntervalFilter.fromListRequest(filter);
        Assert.assertNotNull(devProdFilter);
        Assert.assertEquals(ReportIntervalType.LAST_QUARTER, devProdFilter.getReportInterval());
        Assert.assertEquals(2, devProdFilter.getOuIds().size());
    }
}