package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.services.DocumentationService;
import io.levelops.auth.auth.config.WebSecurityConfig;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.integrations.gcs.models.Category;
import io.levelops.integrations.gcs.models.ReportDocumentation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @RunWith(SpringRunner.class)
// @WebMvcTest(controllers = DocumentationController.class)
// @ContextConfiguration(classes = {DocumentationController.class, DefaultApiTestConfiguration.class, WebSecurityConfig.class})
public class DocumentationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DocumentationService documentationService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before()
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // @Test
    public void getCategoriesListTest() throws Exception {
        Category category = Category.builder().name("zendesk").build();
        List<Category> expected = new ArrayList<>();
        expected.add(category);
        when(documentationService.getCategoriesList(eq(DefaultListRequest.builder().build()))).thenReturn(
                DbListResponse.of(expected, 1));
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/docs/reports/categories/list").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .content("{}")).andReturn()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        verify(documentationService, times(1))
                .getCategoriesList(DefaultListRequest.builder().build());
    }

    // @Test
    public void getReportDocumentsListTest() throws Exception {
        ReportDocumentation report = ReportDocumentation.builder().id("zendesk").build();
        List<ReportDocumentation> expected = new ArrayList<>();
        expected.add(report);
        when(documentationService.getReportDocumentsList(eq(DefaultListRequest.builder().build()))).thenReturn(
                DbListResponse.of(expected, 1));
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/docs/reports/list").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .content("{}")).andReturn()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        verify(documentationService, times(1))
                .getReportDocumentsList(DefaultListRequest.builder().build());
    }

    // @Test
    public void getReportDocTest() throws Exception {
        ReportDocumentation report = ReportDocumentation.builder().build();
        doReturn(report).when(documentationService).getReportDoc(eq("Jira-Zendesk-Report"));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/docs/reports/Jira-Zendesk-Report"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn()));

        verify(documentationService, times(1))
                .getReportDoc("Jira-Zendesk-Report");
    }

}
