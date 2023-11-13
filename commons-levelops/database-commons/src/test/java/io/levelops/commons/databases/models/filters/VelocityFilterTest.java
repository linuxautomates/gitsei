package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;

import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VelocityFilterTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        VelocityFilter expected = VelocityFilter.builder()
                .across(VelocityFilter.DISTINCT.velocity)
                .calculation(VelocityFilter.CALCULATION.ticket_velocity)
                .stacks(List.of(VelocityFilter.STACK.issue_type))
                .build();

        String str = MAPPER.writeValueAsString(expected);
        VelocityFilter actual = MAPPER.readValue(str, VelocityFilter.class);
        //Assert.assertEquals(expected, actual); //ToDo: Fix this
    }

    @Test
    public void testFromListRequest() {
        Map<String, Object> filter = new HashMap<>();
        filter.put("calculation", VelocityFilter.CALCULATION.ticket_velocity.toString());
        filter.put("limit_to_only_applicable_data", true);

        List<Map<String, Object>> sort = new ArrayList<>();
        sort.add(Map.of( "id", "Lead time to first commit", "desc", true));

        DefaultListRequest listRequest = DefaultListRequest.builder()
                .across(VelocityFilter.DISTINCT.velocity.toString())
                .filter(filter)
                .sort(sort)
                .build();
        VelocityFilter velocityFilter = VelocityFilter.fromListRequest(listRequest);
        Assert.assertNotNull(velocityFilter);
        Assert.assertTrue(MapUtils.isNotEmpty(velocityFilter.getSort()));
        Assert.assertTrue(velocityFilter.getSort().containsKey("Lead time to first commit"));
        Assert.assertEquals(SortingOrder.DESC, velocityFilter.getSort().get("Lead time to first commit"));
    }

    @Test
    public void testFromListRequest2() throws JsonProcessingException {
        String str = "{\"page\":0,\"page_size\":10,\"sort\":[{\"id\":\"Lead time to first commit\",\"desc\":false}],\"filter\":{\"jira_projects\":[\"LEV\"],\"velocity_config_id\":\"e5bd4ade-8cc6-41f4-b8f1-a67f527b8590\",\"jira_issue_resolved_at\":{\"$gt\":\"1625529600\",\"$lt\":\"1628294399\"},\"limit_to_only_applicable_data\":true,\"calculation\":\"ticket_velocity\",\"product_id\":\"17\",\"integration_ids\":[\"2\",\"7\"],\"value_stage_names\":[\"Lead time to first commit\"]},\"across\":\"values\"}";
        DefaultListRequest listRequest = MAPPER.readValue(str, DefaultListRequest.class);
        VelocityFilter velocityFilter = VelocityFilter.fromListRequest(listRequest);
        Assert.assertNotNull(velocityFilter);
        Assert.assertTrue(MapUtils.isNotEmpty(velocityFilter.getSort()));
        Assert.assertTrue(velocityFilter.getSort().containsKey("Lead time to first commit"));
        Assert.assertEquals(SortingOrder.ASC, velocityFilter.getSort().get("Lead time to first commit"));
    }

    @Test
    public void testLong() {
        BigDecimal bd = new BigDecimal("1.6409952E9");
        Long l0 = bd.longValue();
        Assert.assertEquals(l0.longValue(), 1640995200l);
        bd = new BigDecimal("1640995200");
        l0 = bd.longValue();
        Assert.assertEquals(l0.longValue(), 1640995200l);
        //Long l1 = Long.valueOf("1.6409952E9");
        //Long l2 = Long.parseLong("1.6409952E9");
    }
}