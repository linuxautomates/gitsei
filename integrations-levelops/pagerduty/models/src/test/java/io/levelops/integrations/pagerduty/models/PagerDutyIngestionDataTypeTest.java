package io.levelops.integrations.pagerduty.models;

import org.junit.Assert;
import org.junit.Test;

public class PagerDutyIngestionDataTypeTest {
    @Test
    public void testDeSerialization() {
        Assert.assertEquals(PagerDutyIngestionDataType.ALERT, PagerDutyIngestionDataType.fromString("alert"));
        Assert.assertEquals(PagerDutyIngestionDataType.ALERT, PagerDutyIngestionDataType.fromString("alerts"));
        Assert.assertEquals(PagerDutyIngestionDataType.ALERT, PagerDutyIngestionDataType.fromString("ALERT"));
        Assert.assertEquals(PagerDutyIngestionDataType.INCIDENT, PagerDutyIngestionDataType.fromString("incident"));
        Assert.assertEquals(PagerDutyIngestionDataType.INCIDENT, PagerDutyIngestionDataType.fromString("incidents"));
        Assert.assertEquals(PagerDutyIngestionDataType.INCIDENT, PagerDutyIngestionDataType.fromString("INCIDENT"));
        Assert.assertEquals(PagerDutyIngestionDataType.LOG_ENTRY, PagerDutyIngestionDataType.fromString("log_entry"));
        Assert.assertEquals(PagerDutyIngestionDataType.LOG_ENTRY, PagerDutyIngestionDataType.fromString("log_entries"));
        Assert.assertEquals(PagerDutyIngestionDataType.LOG_ENTRY, PagerDutyIngestionDataType.fromString("LOG_ENTRY"));
    }
}