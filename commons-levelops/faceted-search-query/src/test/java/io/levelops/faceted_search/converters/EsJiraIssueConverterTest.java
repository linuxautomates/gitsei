package io.levelops.faceted_search.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.faceted_search.db.models.workitems.EsDevProdWorkItemResponse;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class EsJiraIssueConverterTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testGetIssueFromEsWorkItem() throws JsonProcessingException {
        EsWorkItem esWorkItem = MAPPER.readValue("{\"w_id\":\"dcbc612e-8ccd-405a-a74a-963e1786d083\",\"w_workitem_id\":\"PROP-1996\",\"w_integration_id\":11,\"w_workitem_integ_id\":\"PROP-1996_11\",\"w_is_active\":true,\"w_summary\":\"FE - Deployment frequency widget - Advanced settings\",\"w_priority\":\"MEDIUM\",\"w_project\":\"PROP\",\"w_epic\":\"PROP-1505\",\"w_parent_workitem_id\":\"PROP-1505\",\"w_status\":\"DONE\",\"w_workitem_type\":\"STORY\",\"w_story_points\":8.0,\"w_custom_fields\":[{\"name\":\"customfield_10014\",\"str\":\"PROP-1505\"},{\"name\":\"customfield_10020\",\"arr\":[\"PROP Sprint - 202212.1\",\"PROP Sprint - 202211.1\"]},{\"name\":\"customfield_10030\",\"float\":8.0},{\"name\":\"customfield_10067\",\"str\":\"* The filters for the change failure rate widget w\"},{\"name\":\"customfield_10080\",\"str\":\"3 - Moderate\"}],\"w_components\":[],\"w_labels\":[],\"w_assignee\":{\"id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"display_name\":\"Gopal Mandloi\",\"active\":false},\"w_reporter\":{\"id\":\"58b8b559-66e9-4092-b911-12218b6843b2\",\"display_name\":\"yashaswini\"},\"w_first_assignee\":{\"id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"display_name\":\"Gopal Mandloi\"},\"w_desc_size\":0,\"w_hops\":2,\"w_bounces\":1,\"w_num_attachments\":1,\"w_created_at\":1667212,\"w_updated_at\":1670924,\"w_resolved_at\":1670924,\"w_first_assigned_at\":1667822,\"w_first_comment_at\":1668494,\"w_first_attachment_at\":1668494,\"w_age\":162,\"w_unestimated_ticket\":0,\"w_ingested_at\":1681257,\"w_resolution\":\"\",\"w_status_category\":\"Done\",\"w_original_estimate\":0.0,\"w_versions\":[],\"w_fix_versions\":[],\"w_sprint_mappings\":[{\"id\":\"b98aeaaf-a1c6-4415-b12e-7cfb42b5909f\",\"sprint_id\":\"140\",\"added_at\":1667368453,\"planned\":true,\"delivered\":false,\"outside_of_sprint\":false,\"ignorable_workitem_type\":false,\"story_points_planned\":0.0,\"story_points_delivered\":0.0},{\"id\":\"bff99b46-3842-43fa-9ba6-34256dd4d5ef\",\"sprint_id\":\"142\",\"added_at\":1667807425,\"planned\":true,\"delivered\":false,\"outside_of_sprint\":false,\"ignorable_workitem_type\":false,\"story_points_planned\":8.0,\"story_points_delivered\":8.0},{\"id\":\"2aaf44a9-ddf9-40ce-b7e1-b129c4d3b716\",\"sprint_id\":\"144\",\"added_at\":1669729611,\"planned\":true,\"delivered\":true,\"outside_of_sprint\":false,\"ignorable_workitem_type\":false,\"story_points_planned\":8.0,\"story_points_delivered\":8.0}],\"w_links\":[{\"to_workitem_id\":\"PROP-1969\",\"relation\":\"relates to\"}],\"w_salesforce_cases\":[],\"w_priorities_sla\":{\"response_time\":86400,\"solve_time\":86400},\"w_sprints\":[{\"id\":\"144\",\"name\":\"PROP Sprint - 202212.1\",\"state\":\"CLOSED\"},{\"id\":\"142\",\"name\":\"PROP Sprint - 202211.1\",\"state\":\"CLOSED\"}],\"w_hist_assignees\":[{\"assignee\":{\"id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"cloud_id\":\"626ee25f807e0000691abc04\",\"display_name\":\"Gopal Mandloi\"},\"start_time\":1668667,\"end_time\":1681258},{\"assignee\":{\"id\":\"7e327faf-3501-4969-bb41-4588a2a17d47\",\"cloud_id\":\"60b6a61993e3f500716087d9\",\"display_name\":\"Shivam Yadav\"},\"start_time\":1668071,\"end_time\":1668667},{\"assignee\":{\"id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"cloud_id\":\"626ee25f807e0000691abc04\",\"display_name\":\"Gopal Mandloi\"},\"start_time\":1667822,\"end_time\":1668071}],\"w_hist_statuses\":[{\"time_spent\":10419341,\"status\":\"DONE\",\"id\":\"10002\",\"start_time\":1670924,\"end_time\":1681343},{\"time_spent\":120,\"status\":\"READY FOR PROD\",\"id\":\"10073\",\"start_time\":1670924,\"end_time\":1670924},{\"time_spent\":159,\"status\":\"QA IN PROGRESS\",\"id\":\"10099\",\"start_time\":1670924,\"end_time\":1670924},{\"time_spent\":1284716,\"status\":\"READY FOR QA\",\"id\":\"10046\",\"start_time\":1669639,\"end_time\":1670924},{\"time_spent\":8594,\"status\":\"MERGED\",\"id\":\"10054\",\"start_time\":1669631,\"end_time\":1669639},{\"time_spent\":6,\"status\":\"IN REVIEW\",\"id\":\"10001\",\"start_time\":1669631,\"end_time\":1669631},{\"time_spent\":1655448,\"status\":\"IN PROGRESS\",\"id\":\"3\",\"start_time\":1667975,\"end_time\":1669631},{\"time_spent\":762616,\"status\":\"TO DO\",\"id\":\"10061\",\"start_time\":1667212,\"end_time\":1667975}],\"w_hist_sprints\":[{\"id\":\"140\",\"name\":\"DORASprint2\",\"state\":\"FUTURE\",\"goal\":\"\",\"start_time\":1667273,\"end_time\":1668141,\"completed_at\":0},{\"id\":\"142\",\"name\":\"PROP Sprint - 202211.1\",\"state\":\"CLOSED\",\"goal\":\"DORA:  Deliver  Deployment frequency,  Change Failure rate and Lead Time widgets on the dashboard.\\n\\nEY :  Deliver items identified for Milestone 2 for EY.\\n\\nAggs Refactor: Finalize design and approach for the refactor.\\nTrellis enhancements : Identify the bottlenecks and deliver low hanging fruits which deliver relief.\\n\\nCustomer Escalations : On-going\",\"start_time\":1668024,\"end_time\":1669437,\"completed_at\":1669729},{\"id\":\"144\",\"name\":\"PROP Sprint - 202212.1\",\"state\":\"CLOSED\",\"goal\":\"CFR and DF completion -> Done; Lead time -> Merged\",\"start_time\":1669876,\"end_time\":1671236,\"completed_at\":1672999}],\"w_hist_story_points\":[{\"story_points\":0.0,\"start_time\":1667212,\"end_time\":1667557},{\"story_points\":5.0,\"start_time\":1667557,\"end_time\":1667557},{\"story_points\":8.0,\"start_time\":1667557,\"end_time\":1681258}],\"w_hist_state_transitions\":[{\"to_status\":\"DONE\",\"from_status\":\"READY FOR QA\",\"state_transition_time\":1284995},{\"to_status\":\"DONE\",\"from_status\":\"TO DO\",\"state_transition_time\":3711659},{\"to_status\":\"DONE\",\"from_status\":\"IN PROGRESS\",\"state_transition_time\":2949043},{\"to_status\":\"DONE\",\"from_status\":\"READY FOR PROD\",\"state_transition_time\":120},{\"to_status\":\"DONE\",\"from_status\":\"MERGED\",\"state_transition_time\":1293589},{\"to_status\":\"DONE\",\"from_status\":\"IN REVIEW\",\"state_transition_time\":1293595},{\"to_status\":\"DONE\",\"from_status\":\"QA IN PROGRESS\",\"state_transition_time\":279},{\"to_status\":\"READY FOR QA\",\"from_status\":\"TO DO\",\"state_transition_time\":2426664},{\"to_status\":\"READY FOR QA\",\"from_status\":\"IN PROGRESS\",\"state_transition_time\":1664048},{\"to_status\":\"READY FOR QA\",\"from_status\":\"MERGED\",\"state_transition_time\":8594},{\"to_status\":\"READY FOR QA\",\"from_status\":\"IN REVIEW\",\"state_transition_time\":8600},{\"to_status\":\"IN PROGRESS\",\"from_status\":\"TO DO\",\"state_transition_time\":762616},{\"to_status\":\"READY FOR PROD\",\"from_status\":\"READY FOR QA\",\"state_transition_time\":1284875},{\"to_status\":\"READY FOR PROD\",\"from_status\":\"TO DO\",\"state_transition_time\":3711539},{\"to_status\":\"READY FOR PROD\",\"from_status\":\"IN PROGRESS\",\"state_transition_time\":2948923},{\"to_status\":\"READY FOR PROD\",\"from_status\":\"MERGED\",\"state_transition_time\":1293469},{\"to_status\":\"READY FOR PROD\",\"from_status\":\"IN REVIEW\",\"state_transition_time\":1293475},{\"to_status\":\"READY FOR PROD\",\"from_status\":\"QA IN PROGRESS\",\"state_transition_time\":159},{\"to_status\":\"MERGED\",\"from_status\":\"TO DO\",\"state_transition_time\":2418070},{\"to_status\":\"MERGED\",\"from_status\":\"IN PROGRESS\",\"state_transition_time\":1655454},{\"to_status\":\"MERGED\",\"from_status\":\"IN REVIEW\",\"state_transition_time\":6},{\"to_status\":\"IN REVIEW\",\"from_status\":\"TO DO\",\"state_transition_time\":2418064},{\"to_status\":\"IN REVIEW\",\"from_status\":\"IN PROGRESS\",\"state_transition_time\":1655448},{\"to_status\":\"QA IN PROGRESS\",\"from_status\":\"READY FOR QA\",\"state_transition_time\":1284716},{\"to_status\":\"QA IN PROGRESS\",\"from_status\":\"TO DO\",\"state_transition_time\":3711380},{\"to_status\":\"QA IN PROGRESS\",\"from_status\":\"IN PROGRESS\",\"state_transition_time\":2948764},{\"to_status\":\"QA IN PROGRESS\",\"from_status\":\"MERGED\",\"state_transition_time\":1293310},{\"to_status\":\"QA IN PROGRESS\",\"from_status\":\"IN REVIEW\",\"state_transition_time\":1293316}],\"w_integ_type\":\"jira\",\"w_hist_assignee_statuses\":[{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"DONE\",\"issue_status_id\":\"10002\",\"issue_status_category\":\"DONE\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1670924658,\"status_end_time\":1681343999,\"hist_assignee_time\":10333796,\"hist_assignee_time_excluding_resolution\":0,\"interval_start_time\":1670924658,\"interval_end_time\":1681258454},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"READY FOR PROD\",\"issue_status_id\":\"10073\",\"issue_status_category\":\"DONE\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1670924538,\"status_end_time\":1670924658,\"hist_assignee_time\":120,\"hist_assignee_time_excluding_resolution\":0,\"interval_start_time\":1670924538,\"interval_end_time\":1670924658},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"QA IN PROGRESS\",\"issue_status_id\":\"10099\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1670924379,\"status_end_time\":1670924538,\"hist_assignee_time\":159,\"hist_assignee_time_excluding_resolution\":159,\"interval_start_time\":1670924379,\"interval_end_time\":1670924538},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"READY FOR QA\",\"issue_status_id\":\"10046\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1669639663,\"status_end_time\":1670924379,\"hist_assignee_time\":1284716,\"hist_assignee_time_excluding_resolution\":1284716,\"interval_start_time\":1669639663,\"interval_end_time\":1670924379},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"MERGED\",\"issue_status_id\":\"10054\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1669631069,\"status_end_time\":1669639663,\"hist_assignee_time\":8594,\"hist_assignee_time_excluding_resolution\":8594,\"interval_start_time\":1669631069,\"interval_end_time\":1669639663},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"IN REVIEW\",\"issue_status_id\":\"10001\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1669631063,\"status_end_time\":1669631069,\"hist_assignee_time\":6,\"hist_assignee_time_excluding_resolution\":6,\"interval_start_time\":1669631063,\"interval_end_time\":1669631069},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"IN PROGRESS\",\"issue_status_id\":\"3\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1668667024,\"assignee_end_time\":1681258454,\"status_start_time\":1667975615,\"status_end_time\":1669631063,\"hist_assignee_time\":964039,\"hist_assignee_time_excluding_resolution\":964039,\"interval_start_time\":1668667024,\"interval_end_time\":1669631063},{\"historical_assignee\":\"Shivam Yadav\",\"historical_assignee_id\":\"7e327faf-3501-4969-bb41-4588a2a17d47\",\"issue_status\":\"IN PROGRESS\",\"issue_status_id\":\"3\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1668071806,\"assignee_end_time\":1668667024,\"status_start_time\":1667975615,\"status_end_time\":1669631063,\"hist_assignee_time\":595218,\"hist_assignee_time_excluding_resolution\":595218,\"interval_start_time\":1668071806,\"interval_end_time\":1668667024},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"IN PROGRESS\",\"issue_status_id\":\"3\",\"issue_status_category\":\"IN PROGRESS\",\"assignee_start_time\":1667822769,\"assignee_end_time\":1668071806,\"status_start_time\":1667975615,\"status_end_time\":1669631063,\"hist_assignee_time\":96191,\"hist_assignee_time_excluding_resolution\":96191,\"interval_start_time\":1667975615,\"interval_end_time\":1668071806},{\"historical_assignee\":\"Gopal Mandloi\",\"historical_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"issue_status\":\"TO DO\",\"issue_status_id\":\"10061\",\"issue_status_category\":\"TO DO\",\"assignee_start_time\":1667822769,\"assignee_end_time\":1668071806,\"status_start_time\":1667212999,\"status_end_time\":1667975615,\"hist_assignee_time\":152846,\"hist_assignee_time_excluding_resolution\":0,\"interval_start_time\":1667822769,\"interval_end_time\":1667975615}]}", EsWorkItem.class);
        EsDevProdWorkItemResponse esDevProdWorkItemResponse = MAPPER.readValue("{\"workitem_id\":\"PROP-1996\",\"story_points\":8.0,\"historical_assignee_id\":\"7e327faf-3501-4969-bb41-4588a2a17d47\",\"time_in_statuses\":1655454.0,\"assignee_time\":595218.0,\"story_points_portion\":2.88,\"ticket_portion\":0.36}", EsDevProdWorkItemResponse.class);
        DbJiraIssue expected = MAPPER.readValue("{\"id\":\"dcbc612e-8ccd-405a-a74a-963e1786d083\",\"key\":\"PROP-1996\",\"integration_id\":\"11\",\"project\":\"PROP\",\"summary\":\"FE - Deployment frequency widget - Advanced settings\",\"status\":\"DONE\",\"is_active\":true,\"issue_type\":\"STORY\",\"priority\":\"MEDIUM\",\"assignee\":\"Gopal Mandloi\",\"assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"reporter\":\"yashaswini\",\"reporter_id\":\"58b8b559-66e9-4092-b911-12218b6843b2\",\"epic\":\"PROP-1505\",\"parent_key\":\"PROP-1505\",\"desc_size\":0,\"hops\":2,\"bounces\":1,\"num_attachments\":1,\"first_attachment_at\":1668494139,\"issue_created_at\":1667212999,\"issue_updated_at\":1670924658,\"issue_resolved_at\":1670924658,\"issue_due_at\":0,\"original_estimate\":0,\"first_comment_at\":1668494140,\"first_assignee\":\"Gopal Mandloi\",\"first_assignee_id\":\"82f903a7-9044-4348-9996-aaa869c5679f\",\"ingested_at\":1681257600,\"custom_fields\":{\"customfield_10080\":\"3 - Moderate\",\"customfield_10067\":\"* The filters for the change failure rate widget w\",\"customfield_10014\":\"PROP-1505\"},\"labels\":[],\"fix_versions\":[],\"versions\":[],\"component_list\":[],\"status_list\":[{\"status\":\"DONE\",\"status_id\":\"10002\",\"end_time\":1681343999,\"start_time\":1670924658},{\"status\":\"READY FOR PROD\",\"status_id\":\"10073\",\"end_time\":1670924658,\"start_time\":1670924538},{\"status\":\"QA IN PROGRESS\",\"status_id\":\"10099\",\"end_time\":1670924538,\"start_time\":1670924379},{\"status\":\"READY FOR QA\",\"status_id\":\"10046\",\"end_time\":1670924379,\"start_time\":1669639663},{\"status\":\"MERGED\",\"status_id\":\"10054\",\"end_time\":1669639663,\"start_time\":1669631069},{\"status\":\"IN REVIEW\",\"status_id\":\"10001\",\"end_time\":1669631069,\"start_time\":1669631063},{\"status\":\"IN PROGRESS\",\"status_id\":\"3\",\"end_time\":1669631063,\"start_time\":1667975615},{\"status\":\"TO DO\",\"status_id\":\"10061\",\"end_time\":1667975615,\"start_time\":1667212999}],\"assignee_list\":[{\"assignee\":\"Gopal Mandloi\",\"start_time\":1668667024,\"end_time\":1681258454},{\"assignee\":\"Shivam Yadav\",\"start_time\":1668071806,\"end_time\":1668667024},{\"assignee\":\"Gopal Mandloi\",\"start_time\":1667822769,\"end_time\":1668071806}],\"story_points\":8,\"sprint_ids\":[144,142],\"resolution\":\"\",\"status_category\":\"Done\",\"ticket_portion\":0.36,\"story_points_portion\":2.88,\"assignee_time\":595218}", DbJiraIssue.class);

        DbJiraIssue actual = EsJiraIssueConverter.getIssueFromEsWorkItem(esWorkItem, List.of(), esDevProdWorkItemResponse, true, true, true, null);
        Assert.assertEquals(expected.getStoryPointsPortion(), actual.getStoryPointsPortion());

        actual = EsJiraIssueConverter.getIssueFromEsWorkItem(esWorkItem, List.of(), esDevProdWorkItemResponse, true, true, true, "");
        Assert.assertEquals("", actual.getTicketCategory());

        actual = EsJiraIssueConverter.getIssueFromEsWorkItem(esWorkItem, List.of(), esDevProdWorkItemResponse, true, true, true, "test-category-1");
        Assert.assertEquals("test-category-1", actual.getTicketCategory());
    }
}