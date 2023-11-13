//package io.levelops.internal_api.controllers;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.cloud.storage.BlobId;
//import com.google.cloud.storage.Storage;
//import io.levelops.commons.databases.services.GitRepositoryService;
//import io.levelops.commons.databases.services.GitTechnologyService;
//import io.levelops.commons.databases.services.IntegrationService;
//import io.levelops.commons.databases.services.JiraProjectService;
//import io.levelops.commons.databases.services.JiraVersionService;
//import io.levelops.commons.utils.ResourceUtils;
//import io.levelops.internal_api.config.DefaultApiTestConfiguration;
//import io.levelops.internal_api.services.MessagePubService;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
//public class IntegrationDataPushControllerTest {
//    private MockMvc mvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Autowired
//    private GitRepositoryService gitRepositoryService;
//    @Autowired
//    private GitTechnologyService gitTechService;
//    @Autowired
//    private JiraProjectService jiraProjectService;
//    @Autowired
//    private JiraVersionService jiraVersionService;
//    @Autowired
//    private Storage storage;
//    @Autowired
//    private IntegrationService integrationService;
//    @Autowired
//    private MessagePubService messagePubService;
//
//    @Before
//    public void setup() {
//        mvc = MockMvcBuilders.standaloneSetup(new IntegrationDataPushController(objectMapper,
//                gitRepositoryService, storage, gitTechService, jiraProjectService, jiraVersionService,
//                integrationService, messagePubService)).build();
//    }
//
//    @Test
//    public void testPushEndpointWithRepos() throws Exception {
//        when(storage.readAllBytes(BlobId.of("ingestion-levelops",
//                "data/tenant-coke/integration-github/2019/09/30/job-6814b699-75f9-4187-9a44-fbbc0845f5be/repositories/repositories.0.json",
//                1569877718091006L)))
//                .thenReturn(ResourceUtils.getResourceAsString("repositories.json").getBytes());
//        when(gitRepositoryService.batchInsert(any(), any())).thenReturn(List.of("1", "2", "3", "4", "5"));
//        when(gitRepositoryService.deleteForIntegration(anyString(), anyString())).thenReturn(true);
//        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/foo/integrations/1/push")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(ResourceUtils.getResourceAsString("repo_push_request.json"))).andReturn()))
//                .andExpect(status().isAccepted());
//        verify(gitRepositoryService, times(1))
//                .deleteForIntegration("foo", "1");
//        verify(gitRepositoryService, times(1))
//                .batchInsert(eq("foo"), any());
//        verify(gitTechService, times(1))
//                .batchInsert(eq("foo"), any());
//    }
//
//    @Test
//    public void testPushEndpointWithProjects() throws Exception {
//        when(storage.readAllBytes(BlobId.of("ingestion-levelops",
//                "data/tenant-foo/integration-6/2019/10/02/job-ab47aba8-ae92-4a1f-add9-a4ecb8a41793/projects/projects.json",
//                1570046863226785L)))
//                .thenReturn(ResourceUtils.getResourceAsString("projects.json").getBytes());
//        when(jiraProjectService.batchInsert(any(), any())).thenReturn(List.of("1"));
//        when(jiraProjectService.deleteForIntegration(anyString(), anyString())).thenReturn(true);
//        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/foo/integrations/1/push")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(ResourceUtils.getResourceAsString("project_push_request.json"))).andReturn()))
//                .andExpect(status().isAccepted());
//        verify(jiraProjectService, times(1))
//                .deleteForIntegration("foo", "1");
//        verify(jiraProjectService, times(1))
//                .batchInsert(eq("foo"), any());
//        verify(jiraVersionService, times(1))
//                .batchInsert(eq("foo"), any());
//    }
//}
