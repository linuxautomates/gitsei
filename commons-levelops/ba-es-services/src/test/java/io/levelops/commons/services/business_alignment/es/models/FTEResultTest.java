package io.levelops.commons.services.business_alignment.es.models;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class FTEResultTest {
    private static final ObjectMapper M = DefaultObjectMapper.get();

    @Test
    public void testMerge1() throws IOException {
        List<FTEPartial> forCategory = ResourceUtils.getResourceAsList("fte_results/tc1/for_category.json", FTEPartial.class);
        List<FTEPartial> allTickets = ResourceUtils.getResourceAsList("fte_results/tc1/all_tickets.json", FTEPartial.class);
        List<FTEResult> results = FTEResult.merge(FTEResultMergeRequest.builder("KTLO", forCategory, allTickets).build());
        Assert.assertNotNull(results);
        List<FTEResult> expected = ResourceUtils.getResourceAsList("fte_results/tc1/expected.json", FTEResult.class);
        Assert.assertEquals(expected, results);
    }

    @Test
    public void testMerge2() throws IOException {
        List<FTEPartial> forCategory = ResourceUtils.getResourceAsList("fte_results/tc2/for_category.json", FTEPartial.class);
        List<FTEPartial> allTickets = ResourceUtils.getResourceAsList("fte_results/tc2/all_tickets.json", FTEPartial.class);
        List<FTEResult> results = FTEResult.merge(FTEResultMergeRequest.builder("KTLO", forCategory, allTickets).build());
        Assert.assertNotNull(results);
        List<FTEResult> expected = ResourceUtils.getResourceAsList("fte_results/tc2/expected.json", FTEResult.class);
        Assert.assertEquals(expected, results);
    }

    @Test
    public void testMerge3() throws IOException {
        List<FTEPartial> forCategory = ResourceUtils.getResourceAsList("fte_results/tc3/for_category.json", FTEPartial.class);
        List<FTEPartial> allTickets = ResourceUtils.getResourceAsList("fte_results/tc3/all_tickets.json", FTEPartial.class);
        List<FTEResult> results = FTEResult.merge(FTEResultMergeRequest.builder("KTLO", forCategory, allTickets).build());
        Assert.assertNotNull(results);
        List<FTEResult> expected = ResourceUtils.getResourceAsList("fte_results/tc3/expected.json", FTEResult.class);
        Assert.assertEquals(expected, results);
    }

    @Test
    public void testMergeWeeklyDataIntoBiWeekly() throws IOException {
        List<FTEResult> weekly = ResourceUtils.getResourceAsList("fte_results_weekly_to_biweekly/weekly.json", FTEResult.class);
        List<FTEResult> expected = ResourceUtils.getResourceAsList("fte_results_weekly_to_biweekly/biweekly.json", FTEResult.class);
        List<FTEResult> actual = FTEResult.mergeWeeklyDataIntoBiWeekly(weekly);
        Assert.assertEquals(expected, actual);
    }
}