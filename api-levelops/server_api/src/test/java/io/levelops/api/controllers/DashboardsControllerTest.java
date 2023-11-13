package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.model.DashboardDTO;
import io.levelops.auth.auth.authobject.AccessContext;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.models.database.ActivityLog.Action.EDITED;
import static io.levelops.commons.databases.models.database.ActivityLog.TargetItemType.DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class DashboardsControllerTest {
    private static final String COMPANY = "test";
    private MockMvc mvc;
    @Autowired
    private DashboardWidgetService dashboardWidgetService;
    @Autowired
    private ActivityLogService activityLogService;

    @Before
    public void setup() throws SQLException {
        Mockito.reset(dashboardWidgetService, activityLogService);

        DefaultConfigService defaultConfigService = Mockito.mock(DefaultConfigService.class);
        when(defaultConfigService.getDefaultId(any(), any())).thenReturn("");

        UserService userService = Mockito.mock(UserService.class);
        when(userService.get(eq("test"), eq("1"))).thenReturn(Optional.of(User.builder().email("userEmail1").build()));
        when(userService.get(eq("test"), eq("2"))).thenReturn(Optional.of(User.builder().email("userEmail2").build()));


        OUDashboardService ouDashboardService = Mockito.mock(OUDashboardService.class);
        OrgUnitCategoryDatabaseService categoryService = Mockito.mock(OrgUnitCategoryDatabaseService.class);
        when(categoryService.list(eq(COMPANY), any(QueryFilter.class), any(), any())).thenReturn(DbListResponse.of(List.of(OrgUnitCategory.builder()
                .name("name")
                .rootOuId(UUID.randomUUID())
                .rootOuRefId(1)
                .build()), 1));
        OrgUnitsDatabaseService ouService = Mockito.mock(OrgUnitsDatabaseService.class);
        when(ouService.getDashboardsIdsForOuId(eq(COMPANY), any())).thenReturn(DbListResponse.of(List.of(OUDashboard.builder()
                .dashboardId(1)
                .dashboardOrder(1)
                .name("name")
                .build()), 1));

        //The non-standalone setup will require authentication and everything to be done properly.
        mvc = MockMvcBuilders.standaloneSetup(new DashboardsController(
                dashboardWidgetService, defaultConfigService, userService, ouDashboardService, categoryService, ouService, activityLogService, COMPANY, new Auth(true))).build();
    }

    @Test
    public void testCreateDashboard() throws Exception {
        when(dashboardWidgetService.insert(any(), any())).thenReturn("id");
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/dashboards").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", COMPANY)
                        .sessionAttr("session_user", "maxime")
                        .content("{\"name\":\"testdash\", \"type\":\"teams\", \"category\": [\"19cb6c27-4041-4bc4-8c36-f0ab77254b32\"]}")).andReturn()))
                .andExpect(status().isOk())
                .andReturn();
        verify(dashboardWidgetService, times(1))
                .insert(eq("test"),
                        eq(DashboardDTO.builder()
                                .type("teams")
                                .name("testdash")
                                .category(Set.of(UUID.fromString("19cb6c27-4041-4bc4-8c36-f0ab77254b32")))
                                .build()));
        ArgumentCaptor<ActivityLog> activityLogArgumentCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogService, times(1)).insert(eq(COMPANY), activityLogArgumentCaptor.capture());
        assertThat(activityLogArgumentCaptor.getValue().getTargetItem()).isEqualTo("id");
        assertThat(activityLogArgumentCaptor.getValue().getTargetItemType()).isEqualTo(DASHBOARD);
        assertThat(activityLogArgumentCaptor.getValue().getEmail()).isEqualTo("maxime");
    }

    @Ignore
    @Test
    public void testListDashboards() throws Exception {
        when(dashboardWidgetService.listByFilters(eq("test"), any(), any(), any(), any())).thenReturn(
                DbListResponse.of(
                        Collections.singletonList(
                                Dashboard.builder()
                                        .id("1")
                                        .name("testdash")
                                        .ownerId("2")
                                        .type("teams")
                                        .build()), 1));

        mvc.perform(post("/v1/dashboards/list").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "test@propelo.ai")
                        .sessionAttr("user_type", "PUBLIC_DASHBOARD")
                        .sessionAttr("accessContext", AccessContext.builder().build())
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"_metadata\":{\"page_size\":1000,\"page\":0, \"total_count\": 1}," +
                                "\"records\":[{\"name\":\"testdash\",\"type\":\"teams\",\"owner_id\":\"2\"," +
                                "\"id\":\"1\"}]}"))
                .andReturn();
        verify(dashboardWidgetService, times(1))
                .listByFilters(eq("test"), any(), eq(0), eq(1000), any());

        when(dashboardWidgetService.listByFilters(eq("test"), any(), any(), any(), any()))
                .thenReturn(
                        DbListResponse.of(
                                Collections.singletonList(
                                        Dashboard.builder()
                                                .id("1")
                                                .name("testdash")
                                                .type("teams")
                                                .ownerId("2")
                                                .build()), 1));
        mvc.perform(post("/v1/dashboards/list").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "test@propelo.ai")
                        .sessionAttr("user_type", "PUBLIC_DASHBOARD")
                        .sessionAttr("accessContext", AccessContext.builder().build())
                        .content("{\"filter\":{\"type\":\"teams\"," +
                                "\"partial\":{\"name\":\"asd\"}}}"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"_metadata\":{\"page_size\":1000,\"page\":0, \"total_count\":1,}," +
                                "\"records\":[{\"name\":\"testdash\",\"type\":\"teams\",\"owner_id\":\"2\"," +
                                "\"id\":\"1\"}]}"))
                .andReturn();
        ArgumentCaptor<DashboardWidgetService.DashboardFilter> captor = ArgumentCaptor.forClass(DashboardWidgetService.DashboardFilter.class);

        verify(dashboardWidgetService, times(2))
                .listByFilters(eq("test"), captor.capture(), eq(0), eq(1000), any());
        assertThat(captor.getValue().getName()).isEqualTo("asd");
        assertThat(captor.getValue().getType()).isEqualTo("teams");
        assertThat(captor.getValue().getRbacUserEmail()).isEqualTo(null);

        mvc.perform(post("/v1/dashboards/list").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "test@propelo.ai")
                        .sessionAttr("user_type", "PUBLIC_DASHBOARD")
                        .sessionAttr("accessContext", AccessContext.builder().build())
                        .content("{\"filter\":{\"has_rbac_access\":\"true\"}}"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"_metadata\":{\"page_size\":1000,\"page\":0, \"total_count\":1,}," +
                                "\"records\":[{\"name\":\"testdash\",\"type\":\"teams\",\"owner_id\":\"2\"," +
                                "\"id\":\"1\"}]}"))
                .andReturn();
        verify(dashboardWidgetService, times(3))
                .listByFilters(eq("test"), captor.capture(), eq(0), eq(1000), any());
        assertThat(captor.getValue().getRbacUserEmail()).isEqualTo("test@propelo.ai");
        assertThat(captor.getValue().getName()).isEqualTo(null);
        assertThat(captor.getValue().getType()).isEqualTo(null);
    }

    @Test
    public void testGetDashboard() throws Exception {
        when(dashboardWidgetService.get(any(), any())).thenReturn(Optional.of(Dashboard.builder()
                .id("1").name("testdash").build()));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/dashboards/1").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")).andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"testdash\",\"id\":\"1\"}"))
                .andReturn();
        verify(dashboardWidgetService, times(1))
                .get(eq("test"), eq("1"));
    }

    @Ignore // LEV-5063 disabling for now
    @Test
    public void testRbac() throws Exception {
        when(dashboardWidgetService.get(eq("test"), eq("1"))).thenReturn(Optional.empty());
        when(dashboardWidgetService.get(eq("test"), eq("2"))).thenReturn(Optional.of(Dashboard.builder().ownerId("1").build()));
        when(dashboardWidgetService.get(eq("test"), eq("3"))).thenReturn(Optional.of(Dashboard.builder().ownerId("999").build()));

        // -- check that owner_id cannot be changed
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/dashboards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "1")
                        .content("{\"name\":\"edit\", \"owner_id\": \"2\"}")).andReturn()))
                .andExpect(status().isOk())
                .andReturn();

        verify(dashboardWidgetService, times(1))
                .update(eq("test"), eq(Dashboard.builder().id("1").name("edit").build()));

        // -- check that rbac field is protected

        // - dashboard not found
        mvc.perform(put("/v1/dashboards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "1")
                        .content("{\"metadata\": {\"rbac\": { \"test\": true }}}"))
                .andExpect(status().isNotFound());

        // - user found and matching
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/dashboards/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "userEmail1")
                        .content("{\"metadata\": {\"rbac\": { \"test\": true }}}")).andReturn()))
                .andExpect(status().isOk())
                .andReturn();

        // - user found and mis-matching
        mvc.perform(put("/v1/dashboards/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "userEmail2")
                        .content("{\"metadata\": {\"rbac\": { \"test\": true }}}"))
                .andExpect(status().isForbidden());

        // - user not found
        mvc.perform(put("/v1/dashboards/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "test")
                        .sessionAttr("session_user", "1")
                        .content("{\"metadata\": {\"rbac\": { \"test\": true }}}"))
                .andExpect(status().isForbidden());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateDashboard() throws Exception {
        Dashboard dashboardBeforeUpdate = ResourceUtils.getResourceAsObject("dashboards/dashboard_before_update.json", Dashboard.class);
        Dashboard dashboardAfterUpdate = ResourceUtils.getResourceAsObject("dashboards/dashboard_update.json", Dashboard.class);
        when(dashboardWidgetService.get(eq(COMPANY), eq("123"))).thenReturn(
                Optional.of(dashboardBeforeUpdate),
                Optional.of(dashboardAfterUpdate)
        );

        mvc.perform(asyncDispatch(mvc.perform(
                                put("/v1/dashboards/123").contentType(MediaType.APPLICATION_JSON)
                                        .sessionAttr("company", COMPANY)
                                        .sessionAttr("session_user", "maxime")
                                        .content(ResourceUtils.getResourceAsString("dashboards/dashboard_update.json"))
                        )
                        .andReturn()))
                .andExpect(status().isOk())
                .andReturn();

        ArgumentCaptor<ActivityLog> activityLogArgumentCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogService, times(1)).insert(eq(COMPANY), activityLogArgumentCaptor.capture());
        ActivityLog log = activityLogArgumentCaptor.getValue();
        DefaultObjectMapper.prettyPrint(log);
        assertThat(log.getEmail()).isEqualTo("maxime");
        assertThat(log.getTargetItem()).isEqualTo("123");
        assertThat(log.getTargetItemType()).isEqualTo(DASHBOARD);
        assertThat(log.getAction()).isEqualTo(EDITED);

        assertThat(log.getDetails()).containsKey("widgets_diff");
        Map<String, Object> widgetsDiff = (Map<String, Object>) log.getDetails().get("widgets_diff");
        assertThat((Collection<String>) widgetsDiff.get("removed")).containsExactlyInAnyOrder("test2");
        assertThat((Collection<String>) widgetsDiff.get("added")).containsExactlyInAnyOrder("test");

        assertThat(widgetsDiff).containsKey("edited_metadata");
        Map<String, Object> editedMetadata = (Map<String, Object>) widgetsDiff.get("edited_metadata");
        assertThat(editedMetadata).containsKey("3d2eb940-13d8-11ee-aa66-af5f603f67df");

        assertThat(widgetsDiff).containsKey("edited_query");
        Map<String, Object> editedQuery = (Map<String, Object>) widgetsDiff.get("edited_query");
        assertThat(editedQuery).containsKey("b4e79b95-aabf-4755-a157-d5d4e71d5102");
    }
}
