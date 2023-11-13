package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter.TicketCategorizationFilter;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class JiraFilterParserTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Mock
    IntegrationService integService;
    @Mock
    IntegrationTrackingService integrationTrackingService;
    @Mock
    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    @Mock
    JiraFieldConditionsBuilder jiraFieldConditionsBuilder;

    String schemeId = "56294c41-55b9-4ce3-94cf-856130565d7b";

    JiraFilterParser jiraFilterParser;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        jiraFilterParser = new JiraFilterParser(jiraFieldConditionsBuilder, integService, integrationTrackingService, ticketCategorizationSchemeDatabaseService);
        when(integService.listByFilter(eq("foo"), isNull(), anyList(), isNull(), anyList(), anyList(), anyInt(), anyInt()))
                .thenReturn(DbListResponse.of(List.of(), 0));


        when(ticketCategorizationSchemeDatabaseService.get(eq("foo"), eq(schemeId))).thenReturn(Optional.of(TicketCategorizationScheme.builder()
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .categories(Map.of("a", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("a")
                                        .name("A")
                                        .index(10)
                                        .filter(Map.of("labels", List.of("label-a")))
                                        .build(),
                                "b", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("b")
                                        .name("B")
                                        .index(20)
                                        .filter(Map.of("labels", List.of("label-b")))
                                        .build()))
                        .build())
                .build()));
    }

    @Test
    public void testGenerateTicketCategorizationFilters() throws BadRequestException {

        List<TicketCategorizationFilter> filters = jiraFilterParser.generateTicketCategorizationFilters("foo", schemeId);
        DefaultObjectMapper.prettyPrint(filters);
        assertThat(filters).hasSize(2);
        Map<String, TicketCategorizationFilter> filtersById = filters.stream().collect(Collectors.toMap(TicketCategorizationFilter::getId, f -> f));
        assertThat(filtersById).containsOnlyKeys("a", "b");

        assertThat(filtersById.get("a").getId()).isEqualTo("a");
        assertThat(filtersById.get("a").getName()).isEqualTo("A");
        assertThat(filtersById.get("a").getIndex()).isEqualTo(10);
        assertThat(filtersById.get("a").getFilter().getLabels()).containsExactly("label-a");

        assertThat(filtersById.get("b").getId()).isEqualTo("b");
        assertThat(filtersById.get("b").getName()).isEqualTo("B");
        assertThat(filtersById.get("b").getIndex()).isEqualTo(20);
        assertThat(filtersById.get("b").getFilter().getLabels()).containsExactly("label-b");

    }

    @Test
    public void testGenerateTicketCategorizationFilters2() throws IOException, BadRequestException {
        String profileStr = ResourceUtils.getResourceAsString("json/databases/ba/ba_profiles_equifax_1.json");
        TicketCategorizationScheme ticketCategorizationScheme = DefaultObjectMapper.get().readValue(profileStr, TicketCategorizationScheme.class);
        JiraFilterParser parser = new JiraFilterParser(null, null, null, null);
        //List<TicketCategorizationFilter> ticketCategorizationFilters = parser.generateTicketCategorizationFilters("equifax", ticketCategorizationScheme.getConfig().getCategories().values().stream().collect(Collectors.toList()));
        //assertThat(ticketCategorizationFilters).isNotNull();
    }

    @Test
    public void testCreateFilter() throws BadRequestException, SQLException {
        // {"filter":{"ticket_categorization_scheme":"996b996e-4b66-426b-bc6e-78a17ae7a70f"},"across":"ticket_category"}
        JiraIssuesFilter filter = jiraFilterParser.createFilter("foo",
                DefaultListRequest.builder()
                        .filter(Map.of("ticket_categorization_scheme", schemeId))
                        .build(),
                null, null, null, null, false);
        assertThat(filter.getTicketCategorizationFilters()).hasSize(2);
    }

    @Test
    public void testGenerateVelocityFilter() throws IOException, BadRequestException, SQLException {
        String requestString = ResourceUtils.getResourceAsString("velocity/velocity_list_request_1.json");
        DefaultListRequest filter = MAPPER.readValue(requestString, DefaultListRequest.class);
        JiraIssuesFilter jiraIssuesFilter = jiraFilterParser.createFilter("foo", filter, null, null, null, null, true, true);
        assertThat(jiraIssuesFilter).isNotNull();
    }

    @Test
    public void testParseOrDateRange() throws BadRequestException {
        ImmutablePair<Long, Long> issueDueAt;
        issueDueAt = JiraFilterParser.parseOrDateRange(Map.of("issue_due_at", Map.of("$gt", "1626912000", "$lt", "1628208000")), "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1626912000L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208000L);

        issueDueAt = JiraFilterParser.parseOrDateRange(Map.of("issue_due_at", Map.of("$gte", "1626912000", "$lte", "1628208000")), "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1626911999L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208001L);
    }
}