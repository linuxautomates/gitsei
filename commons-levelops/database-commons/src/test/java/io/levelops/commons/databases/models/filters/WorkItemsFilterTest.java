package io.levelops.commons.databases.models.filters;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class WorkItemsFilterTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    
    @Test
    public void test() throws JsonProcessingException, BadRequestException {
        String filterString = "{\"filter\":{\"calculation\":\"ticket_velocity\",\"projects\":[\"project-test-11\"],\"limit_to_only_applicable_data\":false,\"product_id\":\"363\",\"integration_ids\":[\"1011\",\"713\"]},\"across\":\"trend\"}";
        DefaultListRequest filter = MAPPER.readValue(filterString, DefaultListRequest.class);
        WorkItemsFilter wiFilter = WorkItemsFilter.fromDefaultListRequest(filter, null, null);
        Assert.assertNotNull(wiFilter);
        Assert.assertTrue(CollectionUtils.isEmpty(wiFilter.getProjects()));

        filterString = "{\"filter\":{\"calculation\":\"ticket_velocity\",\"workitem_projects\":[\"project-test-11\"],\"limit_to_only_applicable_data\":false,\"product_id\":\"363\",\"integration_ids\":[\"1011\",\"713\"]},\"across\":\"trend\"}";
        filter = MAPPER.readValue(filterString, DefaultListRequest.class);
        wiFilter = WorkItemsFilter.fromDefaultListRequest(filter, null, null);
        Assert.assertNotNull(wiFilter);
        Assert.assertTrue(CollectionUtils.isNotEmpty(wiFilter.getProjects()));
        Assert.assertEquals(1, wiFilter.getProjects().size());
        Assert.assertEquals("project-test-11", wiFilter.getProjects().get(0));


        filterString = "{\"page_size\":0,\"page\":0,\"across\":\"project\",\"filter\":{\"exclude\":{\"workitem_custom_fields\":{}},\"workitem_custom_fields\":{},\"hideScore\":false,\"workitem_types\":[\"Bug\",\"User Story\",\"Feature\"],\"workitem_projects\":[\"spglobal/Platts\"],\"workitem_created_at\":{\"$gt\":\"1623196800\",\"$lt\":\"1631059199\"},\"product_id\":\"1\",\"integration_ids\":[\"1\",\"2\"],\"workitem_sprint_full_names\":[\"Platts\\\\Release 21.08\",\"Platts\\\\Release 21.11\",\"Platts\\\\Release 22.02\",\"Platts\\\\Release 21.01\\\\Sprint 21.01.04\",\"Platts\\\\Release 21.01\\\\Sprint 21.01.05 - IP\",\"Platts\\\\Release 21.04\",\"Platts\\\\Release 21.05\\\\Sprint 21.05.00\",\"Platts\\\\Release 21.05\\\\Sprint 21.05.01\",\"Platts\\\\Release 21.05\\\\Sprint 21.05.02\",\"Platts\\\\Release 21.05\\\\Sprint 21.05.03\",\"Platts\\\\Release 21.05\\\\Sprint 21.05.04\",\"Platts\\\\Release 21.07\"],\"missing_fields\":{\"workitem_story_points\":false}}}";
        filter = MAPPER.readValue(filterString, DefaultListRequest.class);
        wiFilter = WorkItemsFilter.fromDefaultListRequest(filter, null, null);
        Assert.assertNotNull(wiFilter);
        Assert.assertEquals(List.of( "Bug","User Story","Feature"), wiFilter.getWorkItemTypes());
        Assert.assertTrue(CollectionUtils.isNotEmpty(wiFilter.getProjects()));
        Assert.assertEquals(1, wiFilter.getProjects().size());
        Assert.assertEquals("spglobal/Platts", wiFilter.getProjects().get(0));
        //Assert.assertEquals(12, wiFilter);
        Assert.assertEquals(1, wiFilter.getMissingFields().size());
        Assert.assertEquals(false, wiFilter.getMissingFields().get("story_points"));
    }
}