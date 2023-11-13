package io.levelops.commons.services.business_alignment.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.services.business_alignment.es.query_builder.BaJiraWIAggsESQueryBuilder;
import io.levelops.commons.services.business_alignment.es.result_converter.composite.BACompositeESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.result_converter.terms.BATermsESResultConverterFactory;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.services.business_alignment.es.query_builder.BaJiraWIAggsESQueryBuilder.USE_TERMS_AGG;

public class BaJiraWIAggsESQueryBuilderTest {
    private static Map<String, String> AFTER_KEY = null;


    private static final ObjectMapper M = DefaultObjectMapper.get();
    @Test
    public void test() throws IOException {
        JiraIssuesFilter jiraIssuesFilter1 = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/jira_issues_filter_1.json"), JiraIssuesFilter.class);
        jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().issueResolutionRange(ImmutablePair.of(1688169600l, 1690786799l)).build();
        JiraIssuesFilter jiraIssuesFilter2 = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/jira_issues_filter_2.json"), JiraIssuesFilter.class);
        jiraIssuesFilter2 = jiraIssuesFilter2.toBuilder().issueResolutionRange(ImmutablePair.of(1688169600l, 1690786799l)).build();

        OUConfiguration ouConfig = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ou_config.json"), OUConfiguration.class);
        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ticket_categorization_filters.json"), M.getTypeFactory().constructCollectionType(List.class, JiraIssuesFilter.TicketCategorizationFilter.class));
        List<DbJiraField> dbJiraFields = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/db_jira_fields.json"), M.getTypeFactory().constructCollectionType(List.class, DbJiraField.class));

        BaJiraWIAggsESQueryBuilder bldr = new BaJiraWIAggsESQueryBuilder(new BATermsESResultConverterFactory(), new BACompositeESResultConverterFactory());

        //region Tickets Time - Current
        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "KTLO", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Security & Compliance", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Other", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, null, dbJiraFields, USE_TERMS_AGG, AFTER_KEY);
        //endregion

        //region Tickets Count - Current & Prev
        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "KTLO", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Security & Compliance", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Other", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, null, dbJiraFields, USE_TERMS_AGG, AFTER_KEY);
        //endregion

        //region Tickets Count - Current
        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_ASSIGNEE).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, null, dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_ASSIGNEE).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "KTLO", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_ASSIGNEE).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Security & Compliance", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_ASSIGNEE).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Other", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_COUNT, jiraIssuesFilter1, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_ASSIGNEE).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter1.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, null, dbJiraFields, USE_TERMS_AGG, AFTER_KEY);
        //endregion

        //region Tickets Time - Current & Prev
        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter2, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter2.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "KTLO", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter2, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter2.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Security & Compliance", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter2, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter2.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, "Other", dbJiraFields, USE_TERMS_AGG, AFTER_KEY);

        bldr.buildIssuesFTEQuery("fundingcircle", Calculation.TICKET_TIME_SPENT, jiraIssuesFilter2, ouConfig,
                BaJiraOptions.builder().attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES).build(), ESAggInterval.fromStringWithDefault(jiraIssuesFilter2.getAggInterval(), ESAggInterval.MONTH),
                ticketCategorizationFilters, null, dbJiraFields, USE_TERMS_AGG, AFTER_KEY);
        //endregion
    }
}