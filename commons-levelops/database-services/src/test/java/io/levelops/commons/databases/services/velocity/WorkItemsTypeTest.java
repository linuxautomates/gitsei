package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Assert;
import org.junit.Test;

public class WorkItemsTypeTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromString() {
        for(WorkItemsType wt : WorkItemsType.values()) {
            Assert.assertEquals(wt, WorkItemsType.fromString(wt.toString()));
            Assert.assertEquals(wt, WorkItemsType.fromString(wt.toString().toLowerCase()));
            Assert.assertEquals(wt, WorkItemsType.fromString(wt.toString().toUpperCase()));
        }
    }

    @Test
    public void testFromListRequest() throws JsonProcessingException {
        Assert.assertNull(WorkItemsType.fromListRequest(DefaultListRequest.builder().build()));
        Assert.assertEquals(null, WorkItemsType.fromListRequest(MAPPER.readValue("{ \"filter\": { \"work_items_type\": \"junk\" } }", DefaultListRequest.class)));
        Assert.assertEquals(WorkItemsType.JIRA, WorkItemsType.fromListRequest(MAPPER.readValue("{ \"filter\": { \"work_items_type\": \"jira\" } }", DefaultListRequest.class)));
        Assert.assertEquals(WorkItemsType.WORK_ITEM, WorkItemsType.fromListRequest(MAPPER.readValue("{ \"filter\": { \"work_items_type\": \"work_item\" } }", DefaultListRequest.class)));
        Assert.assertEquals(WorkItemsType.JIRA_AND_WORK_ITEM, WorkItemsType.fromListRequest(MAPPER.readValue("{ \"filter\": { \"work_items_type\": \"jira_and_work_item\" } }", DefaultListRequest.class)));
        Assert.assertEquals(WorkItemsType.NONE, WorkItemsType.fromListRequest(MAPPER.readValue("{ \"filter\": { \"work_items_type\": \"none\" } }", DefaultListRequest.class)));
    }
}