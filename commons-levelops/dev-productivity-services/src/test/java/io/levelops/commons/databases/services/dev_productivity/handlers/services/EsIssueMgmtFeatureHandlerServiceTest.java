package io.levelops.commons.databases.services.dev_productivity.handlers.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.jackson.DefaultObjectMapper;
import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EsIssueMgmtFeatureHandlerServiceTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String COMPANY = "githubaggs";

//    @Mock
//    ESClientFactory esClientFactory;
//    @Mock
//    ElasticsearchClient esClient;


    EsIssueMgmtFeatureHandlerService handlerService;

//    @Before
//    public void setUp() throws Exception {
//        MockitoAnnotations.initMocks(this);
//        handlerService = new EsIssueMgmtFeatureHandlerService(esClientFactory, Set.of(COMPANY), Set.of(COMPANY));
//        Mockito.when(esClientFactory.getESClient(COMPANY)).thenReturn(esClient);
//
//    }

    @Test
    public void test() throws IOException, GeneralSecurityException {
        ESClusterInfo esClusterInfo1 = ESClusterInfo.builder()
                .name("CLUSTER_1")
                .ipAddresses(List.of("cluster1-es-http.default.es.local")).port(9200)
                .userName("elastic").password("pass_1")
                .defaultCluster(true)
                .sslCertPath("cert1.crt")
                .build();
        ESClusterInfo esClusterInfo2 = ESClusterInfo.builder()
                .name("CLUSTER_2")
                .ipAddresses(List.of("cluster2-es-http.default.es.local")).port(9200)
                .userName("elastic").password("pass_2")
                .defaultCluster(false)
                .sslCertPath("cert2.crt")
                .build();
        ESClientFactory esClientFactory = new ESClientFactory(List.of(esClusterInfo1, esClusterInfo2));

        handlerService = new EsIssueMgmtFeatureHandlerService(esClientFactory, Set.of(COMPANY), Set.of(COMPANY));

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(JiraIssuesFilter.DISTINCT.issue_resolved).aggInterval("month")
                .ingestedAt(1680480000l).ingestedAtByIntegrationId(Map.of("11", 1680480000l))
                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                .assignees(List.of("c40ecf7f-563d-4503-b1b9-d7ec3bd6e3e7"))
                .integrationIds(List.of("11"))
                .statusCategories(List.of("DONE","COMPLETED","RESOLVED","Done","Completed","Resolved"))
                .issueResolutionRange(ImmutablePair.of(1672531200l, 1680307199l))
                .ticketCategories(List.of("Bugs","New Features"))
                .build();
        //ToDo: Fix Unit Test
        //handlerService.getJiraFeatureBreakDown(COMPANY, jiraIssuesFilter, List.of(), DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH, Map.of(), 0, 1000);
    }

}