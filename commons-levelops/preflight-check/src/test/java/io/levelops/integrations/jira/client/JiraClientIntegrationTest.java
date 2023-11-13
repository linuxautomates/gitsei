package io.levelops.integrations.jira.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.jira.models.JiraApiSearchQuery;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraCommentsResult;
import io.levelops.integrations.jira.models.JiraComponent;
import io.levelops.integrations.jira.models.JiraCreateIssueFields;
import io.levelops.integrations.jira.models.JiraCreateMeta;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraServerInfo;
import io.levelops.integrations.jira.models.JiraUpdateIssue;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraVersion;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JiraClientIntegrationTest {

    private JiraClientFactory jiraClientFactory;
    private static final IntegrationKey KEY = IntegrationKey.builder().tenantId("coke").integrationId("jira").build();

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
//        InventoryServiceImpl inventoryService = new InventoryServiceImpl("http://localhost:9999", okHttpClient, DefaultObjectMapper.get());
        InMemoryInventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey("coke", "jira", "jira", "https://levelops.atlassian.net", null,System.getenv("USERNAME"), System.getenv("API_KEY"))
                .build());

//        InMemoryInventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
//                .oauth1Token("coke", "jira", "jira", "http://localhost:8080", null,
//                        System.getenv("PRIVATE_KEY"), System.getenv("CONSUMER_KEY"), System.getenv("VERIFICATION_CODE"), System.getenv("ACCESS_TOKEN"))
//                .build());

        jiraClientFactory = JiraClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .allowUnsafeSSL(true)
                .build();
    }

    @Test
    public void projects() throws JiraClientException {
        List<JiraProject> projects = jiraClientFactory.get(KEY).getProjects();
        DefaultObjectMapper.prettyPrint(projects);
    }

    @Test
    public void project() throws JiraClientException {
        JiraProject project = jiraClientFactory.get(KEY).getProject("LEV");
        DefaultObjectMapper.prettyPrint(project);
    }

    @Test
    public void versions() throws JiraClientException {
        List<JiraVersion> versions = jiraClientFactory.get(KEY).getProjectVersions("LEV");
        DefaultObjectMapper.prettyPrint(versions);
    }

    @Test
    public void issues() throws JiraClientException {
        JiraApiSearchResult search = jiraClientFactory.get(KEY).search(JiraApiSearchQuery.builder()
                .expand(Set.of("changelog"))
                .jql("key=LEV-63")
                .fields(Set.of("*all"))
                .maxResults(1)
                .build());
        DefaultObjectMapper.prettyPrint(search.getIssues());
    }

    @Test
    public void issueComments() throws JiraClientException {
        JiraCommentsResult output = jiraClientFactory.get(KEY).getIssueComments("LEV-63");
        DefaultObjectMapper.prettyPrint(output);
    }

    @Test
    public void changelog() throws JiraClientException {
        Map<String, Object> out = jiraClientFactory.get(KEY).getIssueChangeLog("LEV-2814", 0);
        DefaultObjectMapper.prettyPrint(out);
    }

    // TODO not supported yet
//    @Test
//    public void backlog() {
//        List<Map> out = jiraClientFactory.get(KEY).getBacklog();
//    }

    @Test
    public void fields() throws JiraClientException {
        List<JiraField> fields = jiraClientFactory.get(KEY).getFields();
        DefaultObjectMapper.prettyPrint(fields);
        DefaultObjectMapper.prettyPrint(fields);
    }

    @Test
    public void userSearch() throws JiraClientException {
        JiraServerInfo serverInfo = jiraClientFactory.get(KEY).getServerInfo();
        DefaultObjectMapper.prettyPrint(serverInfo);

        Object out = jiraClientFactory.get(KEY).searchUsers("maxime", serverInfo.getDeploymentType());
        DefaultObjectMapper.prettyPrint(out);

//        String pk = "LEV";
        String pk = "TS";
        JiraCreateMeta meta = jiraClientFactory.get(KEY).getCreateMeta(List.of(pk));
        DefaultObjectMapper.prettyPrint(meta);

        JiraProject project = jiraClientFactory.get(KEY).getProject(pk);
        DefaultObjectMapper.prettyPrint(project);

        // cloud
        var fields = JiraCreateIssueFields.builder()
                .summary("automated test issue")
                .description("please ignore")
                .project(JiraProject.builder()
                        .id("10001")
                        .build())
                .issueType(JiraIssueType.builder()
                        .id(10004L)
                        .build())
                .assignee(JiraUser.builder()
                        .accountId("5d6049c4c812c40d27c01ea9")
                        .build())
                .labels(List.of("test"))
                .components(List.of(JiraComponent.builder()
                        .id("10002")
                        .build()))
                .build();

        // server
        fields = JiraCreateIssueFields.builder()
                .summary("automated test issue")
                .description("please ignore")
                .project(JiraProject.builder()
                        .id("10000")
                        .build())
                .issueType(JiraIssueType.builder()
                        .id(10004L)
                        .build())
                .assignee(JiraUser.builder()
                        .name("maxime")
                        .build())
                .labels(List.of("test", "a", "b"))
                .components(List.of(JiraComponent.builder()
                        .id("10002")
                        .build(),
                        JiraComponent.builder()
                                .id("10001")
                                .build()))
                .build();

//        JiraIssue issue = jiraClientFactory.get(KEY).createIssue(fields);
//        DefaultObjectMapper.prettyPrint(issue);

    }

    @Test
    public void testUpdate() throws JiraClientException {
        jiraClientFactory.get(KEY).editIssue("LEV-1125", JiraUpdateIssue.builder()
                .labels(List.of(
                        Map.of(JiraUpdateIssue.JiraUpdateOp.ADD, "test2"),
                        Map.of(JiraUpdateIssue.JiraUpdateOp.ADD, "test3"),
                        Map.of(JiraUpdateIssue.JiraUpdateOp.REMOVE, "test")))
                .summary(List.of(Map.of(JiraUpdateIssue.JiraUpdateOp.SET, "automated issue (edit)")))
                .description(List.of(Map.of(JiraUpdateIssue.JiraUpdateOp.SET, "test please ignore (edit)")))
                .assignee(List.of(Map.of(JiraUpdateIssue.JiraUpdateOp.SET, JiraUser.builder()
                        .accountId("5d6049c4c812c40d27c01ea9")
                        .build())))
                .duedate(List.of(Map.of(JiraUpdateIssue.JiraUpdateOp.SET, "2021-03-16")))
                .build());
    }

    @Test
    public void testUpdateCustom() throws JiraClientException {
        var o = jiraClientFactory.get(KEY).getTransitions("TEST1-732");
        DefaultObjectMapper.prettyPrint(o);

        jiraClientFactory.get(KEY).doTransition("TEST1-732", JiraUpdateIssue.builder()
                .build(), "21");

        var update = JiraUpdateIssue.builder()
//                .setCustomField("customfield_10040", Map.of("value", "Very Wow"))
                .setCustomField("customfield_10041", "def")
                .build();

        DefaultObjectMapper.prettyPrint(update);
        jiraClientFactory.get(KEY).editIssue("TEST1-732", update);

    }

    @Test
    public void testAddWatcher() throws JiraClientException {
        jiraClientFactory.get(KEY).addWatcher("LEV-1125", "5d6049c417bc890d28ef70dd");
    }

    @Test
    public void testCreateVersion() throws JiraClientException {
        JiraVersion version = jiraClientFactory.get(KEY).createVersion("10001", "test-version2");
        System.out.println(version.getId());
    }

    @Test
    public void testCreateIssueWithPriority() throws JiraClientException {
        JiraIssue issue = jiraClientFactory.get(KEY).createIssue(JiraCreateIssueFields.builder()
                .summary("test please ignore")
                .issueType(JiraIssueType.builder()
                        .id(10002L)
                        .build())
                .project(JiraProject.builder()
                        .id("10001")
                        .build())
                .priority(JiraIssueFields.JiraPriority.builder()
                        .name("Highest")
                        .build())
                .build());
        DefaultObjectMapper.prettyPrint(issue);
    }

    @Test
    public void testUpdatePriority() throws JiraClientException {
        jiraClientFactory.get(KEY).editIssue("LEV-2783", JiraUpdateIssue.builder()
                .setPriority(JiraIssueFields.JiraPriority.builder().name("Lowest").build())
//                .fixVersions(List.of(Map.of(JiraUpdateIssue.JiraUpdateOp.ADD, Map.of("name", "test-fix-version-update"))))
                .build());
    }

    @Test
    public void getStatuses() throws JiraClientException {
        List<JiraIssueFields.JiraStatus> statuses = jiraClientFactory.get(KEY).getStatuses();
        statuses.forEach(s -> System.out.println(s.getName() + " -> " + s.getStatusCategory().getName()));
    }
}