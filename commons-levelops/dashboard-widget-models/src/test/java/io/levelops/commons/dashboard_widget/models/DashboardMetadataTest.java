package io.levelops.commons.dashboard_widget.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;


public class DashboardMetadataTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerializeDeserialize() throws JsonProcessingException {
        String serialized = "{ \"ou_ids\": [ \"194\" ], \"dashboard_time_range\": true, \"show_org_unit_selection\": true, \"dashboard_time_range_filter\": \"last_30_days\" }";
        DashboardMetadata dashboardMetadata = MAPPER.readValue(serialized, DashboardMetadata.class);
        Assert.assertNotNull(dashboardMetadata);
        Assert.assertEquals(true, dashboardMetadata.getDashboardTimeRange());
        Assert.assertEquals(true, dashboardMetadata.getShowOrgUnitSelection());
        Assert.assertEquals(ReportIntervalType.LAST_30_DAYS, dashboardMetadata.getDashboardTimeRangeFilter());
    }
}