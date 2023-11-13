package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.notification.services.NotificationService;
import io.levelops.api.services.TagItemService;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.BestPracticesService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.MsgTemplateService;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class BestPracticesControllerTest {
    private MockMvc mvc;

    @Mock
    private BestPracticesService bPracticesService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private NotificationService notificationsService;
    @Mock
    private TagItemService tagItemService;
    @Mock
    private MsgTemplateService msgTemplateService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mvc = MockMvcBuilders
                .standaloneSetup(new BestPracticesController(bPracticesService, tagItemService, msgTemplateService,
                        activityLogService, notificationsService, integrationService, "https://test.test.test"))
                .build();
    }

    @Test
    public void testDeleteQuestionnaireTemplate() throws Exception {
        String id1 = "9927cf9f-3ddd-41b5-9d24-475a0b97d232";
        when(bPracticesService.update(anyString(), any())).thenReturn(true);
        when(bPracticesService.deleteAndReturn(anyString(), eq(id1))).thenReturn(Optional.of(
                BestPracticesItem.builder()
                        .name("")
                        .build()));
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/bestpractices/" + id1)
                .sessionAttr("session_user", "a")
                .sessionAttr("company", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"google KB1\",\"tags\":[\"16\",\"15\"],\"type\":\"LINK\",\"value\":\"https://www.google.com\"}"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
        ArgumentCaptor<BestPracticesItem> captor = ArgumentCaptor.forClass(BestPracticesItem.class);
        verify(bPracticesService).update(anyString(), captor.capture());
        List<BestPracticesItem> updates = captor.getAllValues();
        Assert.assertNotNull("BestPractices updates need the id but the BestPracticesItem provided did not have any.", updates.get(0).getId());
        Assert.assertEquals(id1, updates.get(0).getId().toString());
    }
}
