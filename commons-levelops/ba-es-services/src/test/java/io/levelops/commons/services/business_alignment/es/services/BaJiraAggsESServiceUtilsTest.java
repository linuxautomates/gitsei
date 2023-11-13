package io.levelops.commons.services.business_alignment.es.services;

import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BaJiraAggsESServiceUtilsTest {
    private static final List<String> ALL_CATEGORY_NAMES = List.of("n1", "n2", "n3", "Other");
    @Test
    public void testGetEffectiveCategoryNames() {
        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(
                JiraIssuesFilter.TicketCategorizationFilter.builder().name("n1").build(),
                JiraIssuesFilter.TicketCategorizationFilter.builder().name("n2").build(),
                JiraIssuesFilter.TicketCategorizationFilter.builder().name("n3").build()
        );
        Assert.assertEquals(ALL_CATEGORY_NAMES, BaJiraAggsESServiceUtils.getEffectiveCategoryNames(null, ticketCategorizationFilters));
        Assert.assertEquals(ALL_CATEGORY_NAMES, BaJiraAggsESServiceUtils.getEffectiveCategoryNames("", ticketCategorizationFilters));
        Assert.assertEquals(ALL_CATEGORY_NAMES, BaJiraAggsESServiceUtils.getEffectiveCategoryNames(" ", ticketCategorizationFilters));
        Assert.assertEquals(List.of("n1"), BaJiraAggsESServiceUtils.getEffectiveCategoryNames("n1", ticketCategorizationFilters));
        Assert.assertEquals(List.of("n2"), BaJiraAggsESServiceUtils.getEffectiveCategoryNames("n2", ticketCategorizationFilters));
        Assert.assertEquals(List.of("n3"), BaJiraAggsESServiceUtils.getEffectiveCategoryNames("n3", ticketCategorizationFilters));
        Assert.assertEquals(List.of("Other"), BaJiraAggsESServiceUtils.getEffectiveCategoryNames("Other", ticketCategorizationFilters));

        try {
            BaJiraAggsESServiceUtils.getEffectiveCategoryNames("Doesnt_Exist", ticketCategorizationFilters);
            Assert.fail("Runtime Exception expected!");
        } catch (RuntimeException e) {
            Assert.assertEquals("Category Name Doesnt_Exist does not exist in the profile!", e.getMessage());
        }
    }
}