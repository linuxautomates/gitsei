package io.levelops.commons.databases.services.jira.utils;

import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueReadUtilsTest {

//    @Test
    public void needParentIssueTypeJoin() {
        assertThat(JiraIssueReadUtils.needParentIssueTypeJoin(JiraIssuesFilter.builder().build())).isFalse();
        assertThat(JiraIssueReadUtils.needParentIssueTypeJoin(JiraIssuesFilter.builder().parentIssueTypes(List.of()).build())).isFalse();
        assertThat(JiraIssueReadUtils.needParentIssueTypeJoin(JiraIssuesFilter.builder().excludeParentIssueTypes(List.of()).build())).isFalse();
        assertThat(JiraIssueReadUtils.needParentIssueTypeJoin(JiraIssuesFilter.builder().parentIssueTypes(List.of("TEST")).build())).isTrue();
        assertThat(JiraIssueReadUtils.needParentIssueTypeJoin(JiraIssuesFilter.builder().excludeParentIssueTypes(List.of("TEST")).build())).isTrue();

        assertThat(JiraIssueReadUtils.needParentIssueTypeJoin(JiraIssuesFilter.builder()
                .ticketCategorizationFilters(List.of(
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .filter(JiraIssuesFilter.builder().parentIssueTypes(List.of("TEST")).build())
                        .build()))
                .build())).isTrue();

    }

}