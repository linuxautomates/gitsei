package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.services.QuestionnaireTemplateDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.services.QuestionnaireTemplateService;
import io.levelops.internal_api.services.TagItemService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        IntegrationsController.class,
        DefaultApiTestConfiguration.class
})
public class QuestionnaireTemplateControllerTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private MockMvc mvc;

    @Mock
    private TagItemService tagItemService;
    @Mock
    private QuestionnaireTemplateDBService dbService;
    private QuestionnaireTemplateService qTemplateService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        qTemplateService = new QuestionnaireTemplateService(tagItemService,dbService);
        mvc = MockMvcBuilders
            .standaloneSetup(new QuestionnaireTemplateController(qTemplateService))
            .build();
    }

    @Test
    public void testCreateQuestionnaireTemplate() throws Exception {
        QuestionnaireTemplate qt = QuestionnaireTemplate.builder()
                .name("qt 1")
                .lowRiskBoundary(40)
                .midRiskBoundary(80)
                .sections(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()))
                .tagIds(Arrays.asList("tag1", "tag2"))
                .riskEnabled(true)
                .build();
        String qtString = MAPPER.writeValueAsString(qt);

        ArgumentCaptor<QuestionnaireTemplate> questionnaireTemplateCaptor = ArgumentCaptor.forClass(QuestionnaireTemplate.class);
        when(tagItemService.batchInsert(eq("test"),any(), eq(QUESTIONNAIRE_TEMPLATE), eq(Arrays.asList("tag1", "tag2")))).thenReturn(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        doReturn(UUID.randomUUID().toString()).when(dbService).insert(eq("test"), questionnaireTemplateCaptor.capture());

        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/qtemplates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(qtString))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        QuestionnaireTemplate c = questionnaireTemplateCaptor.getValue();
        Assert.assertEquals(qt.getName(), c.getName());
        Assert.assertEquals(qt.getLowRiskBoundary(), c.getLowRiskBoundary());
        Assert.assertEquals(qt.getMidRiskBoundary(), c.getMidRiskBoundary());
        Assert.assertEquals(qt.getRiskEnabled(), c.getRiskEnabled());
        Assert.assertEquals(qt.getSections(), c.getSections());
    }

    @Test
    public void testDeleteQuestionnaireTemplate() throws Exception  {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        when(dbService.checkIfUsedInQuestionnaires(anyString(), anyString())).thenReturn(Optional.empty());
        when(dbService.deleteAndReturn(anyString(), eq(id1))).thenReturn(Optional.of(
            QuestionnaireTemplate.builder()
                .id("")
                .build()));
        when(dbService.checkIfUsedInQuestionnaires(anyString(), eq(id2))).thenReturn(Optional.of(List.of("2", "3")));
        mvc.perform(asyncDispatch(mvc.perform(delete("/internal/v1/tenants/test/qtemplates/"+id1))
                .andReturn()))
                .andExpect(status().is(200));
        mvc.perform(asyncDispatch(mvc.perform(delete("/internal/v1/tenants/test/qtemplates/"+id2))
                .andReturn()))
                .andExpect(status().is(200));
        verify(dbService, times(1)).deleteAndReturn(anyString(), anyString());
        verify(dbService, times(2)).checkIfUsedInQuestionnaires(anyString(), anyString());
    }
}
