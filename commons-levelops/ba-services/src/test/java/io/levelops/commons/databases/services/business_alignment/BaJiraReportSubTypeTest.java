package io.levelops.commons.databases.services.business_alignment;

import org.junit.Assert;
import org.junit.Test;

public class BaJiraReportSubTypeTest {
    @Test
    public void test() {
        String baProfileId = "8dfe6bed-17ed-4785-a038-dd2dc67dc064";
        String baCategory = "Non Capitalizable";
        Assert.assertEquals("effort_investment_time_spent_8dfe6bed-17ed-4785-a038-dd2dc67dc064_Non Capitalizable", BaJiraReportSubType.generateReportSubType("effort_investment_time_spent", baProfileId, baCategory));
        Assert.assertEquals("tickets_report_8dfe6bed-17ed-4785-a038-dd2dc67dc064_Non Capitalizable", BaJiraReportSubType.generateReportSubType("tickets_report", baProfileId, baCategory));
        Assert.assertEquals("story_point_report_8dfe6bed-17ed-4785-a038-dd2dc67dc064_Non Capitalizable", BaJiraReportSubType.generateReportSubType("story_point_report", baProfileId, baCategory));
        Assert.assertEquals("commit_count_fte_8dfe6bed-17ed-4785-a038-dd2dc67dc064_Non Capitalizable", BaJiraReportSubType.generateReportSubType("commit_count_fte", baProfileId, baCategory));
    }
}