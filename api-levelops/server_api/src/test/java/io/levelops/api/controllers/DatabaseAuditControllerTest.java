package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.controllers.monitoring.DatabaseAuditController;
import io.levelops.api.services.GcpLoggingService;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.models.ListResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static io.levelops.api.controllers.monitoring.DatabaseAuditController.CLOUD_SQL_RESOURCE;
import static io.levelops.api.controllers.monitoring.DatabaseAuditController.POSTGRES_LOGNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class DatabaseAuditControllerTest {

  @Autowired
  private GcpLoggingService gcpLoggingService;

  @Autowired
  private ActivityLogService activityLogService;

  @Autowired
  Clock clock;

  DatabaseAuditController databaseAuditController;
  private MockMvc mvc;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    databaseAuditController = new DatabaseAuditController(activityLogService, gcpLoggingService, clock);
    mvc = MockMvcBuilders.standaloneSetup(databaseAuditController).build();
  }

  @After
  public void cleanup() {
    Mockito.clearInvocations(gcpLoggingService);
  }

  @Test
  public void testGetDbAuditLogs_noTimeInputs() throws Exception {
    Instant instant = Instant.ofEpochMilli(1692057600000l);
    when(clock.instant()).thenReturn(instant);
    List<String> logEntries = new ArrayList<>();
    logEntries.add("2023-08-21 06:36:22.007 UTC [815008]: [2-1] db=postgres,user=praveen@harness.io STATEMENT:  select");
    logEntries.add("2023-08-17 13:28:32.829 UTC [381418]: [4-1]" +
            " db=postgres,user=nishith.patel@harness.io STATEMENT:  Select r.* from fis.cicd_jobs");
    when(gcpLoggingService.getLogs(anyString(), anyString(), anyString(), any(Instant.class),
            any(Instant.class))).thenReturn(logEntries);

    String company = "capitalone";

    mvc.perform(get("/v1/db-audit/db-audit-log")
            .sessionAttr("company", company)
            ).andExpect(status().is(200));

    String userFilter = "NOT textPayload :(\"user=operations\" OR \"user=postgres\"" +
            " OR \"user=cloudsqladmin\" OR \"user= \" OR \"user=[unknown]\")";
    String companyFilter = "textPayload :(\" " + company + ".\")";
    String additionalFilters = userFilter + " AND " + companyFilter;

    verify(gcpLoggingService, times(1)).getLogs(POSTGRES_LOGNAME, CLOUD_SQL_RESOURCE,
            additionalFilters, instant.minus(1, ChronoUnit.DAYS), instant);
  }

  @Test
  public void testGetDbAuditLogs_validateContent() throws Exception {
    Instant instant = Instant.ofEpochMilli(1692057600000l);
    when(clock.instant()).thenReturn(instant);
    List<String> logEntries = new ArrayList<>();
    logEntries.add("2023-08-21 06:36:22.007 UTC [815008]: [2-1] db=postgres,user=praveen@harness.io STATEMENT:  select");
    logEntries.add("2023-08-17 13:28:32.829 UTC [381418]: [4-1]" +
            " db=postgres,user=nishith.patel@harness.io STATEMENT:  Select r.* from fis.cicd_jobs");
    when(gcpLoggingService.getLogs(anyString(), anyString(), anyString(), any(Instant.class),
            any(Instant.class))).thenReturn(logEntries);

    String company = "capitalone";

    ResponseEntity<ListResponse<String>> response = databaseAuditController
            .getDbAuditLogForTenant(null, null, "capitalone");

    Assert.assertNotNull(response);
    Assert.assertEquals(2, response.getBody().getRecords().size());
    Assert.assertTrue(response.getBody().getRecords().contains("user=praveen@harness.io  accessed the database at 2023-08-21 06:36:22.007  UTC"));
    Assert.assertTrue(response.getBody().getRecords().contains("user=nishith.patel@harness.io  accessed the database at 2023-08-17 13:28:32.829  UTC"));

  }

  @Test
  public void testGetDbAuditLogs_validTimeInputs() throws Exception {
    Instant instant = Instant.ofEpochMilli(1692057600000l);
    when(clock.instant()).thenReturn(instant);
    List<String> logEntries = new ArrayList<>();
    logEntries.add("2023-08-21 06:36:22.007 UTC [815008]: [2-1] db=postgres,user=praveen@harness STATEMENT:  select");
    logEntries.add("2023-08-17 13:28:32.829 UTC [381418]: [4-1]" +
            " db=postgres,user=nishith.patel@harness.io STATEMENT:  Select r.* from fis.cicd_jobs");
    when(gcpLoggingService.getLogs(anyString(), anyString(), anyString(), any(Instant.class),
            any(Instant.class))).thenReturn(logEntries);

    String company = "capitalone";
    Instant start = instant.minus(4, ChronoUnit.DAYS);
    Instant end = instant.minus(1, ChronoUnit.DAYS);

    mvc.perform(get("/v1/db-audit/db-audit-log")
                    .sessionAttr("company", company)
                    .queryParam("startTime", String.valueOf(start.toEpochMilli()))
                    .queryParam("endTime", String.valueOf(end.toEpochMilli()))
            ).andExpect(status().is(200))
            .andReturn();

    String userFilter = "NOT textPayload :(\"user=operations\" OR \"user=postgres\"" +
            " OR \"user=cloudsqladmin\" OR \"user= \" OR \"user=[unknown]\")";
    String companyFilter = "textPayload :(\" " + company + ".\")";
    String additionalFilters = userFilter + " AND " + companyFilter;

    verify(gcpLoggingService, times(1)).getLogs(POSTGRES_LOGNAME, CLOUD_SQL_RESOURCE,
            additionalFilters, start, end);
  }

  @Test
  public void testGetDbAuditLogs_exception() throws Exception {
    Instant instant = Instant.ofEpochMilli(1692057600000l);
    when(clock.instant()).thenReturn(instant);
    List<String> logEntries = new ArrayList<>();
    logEntries.add("2023-08-21 06:36:22.007 UTC [815008]: [2-1] db=postgres,user=praveen@harness STATEMENT:  select");
    when(gcpLoggingService.getLogs(anyString(), anyString(), anyString(), any(Instant.class),
            any(Instant.class))).thenThrow(new RuntimeException("exception"));

    String company = "capitalone";
    Instant start = instant.minus(4, ChronoUnit.DAYS);
    Instant end = instant.minus(1, ChronoUnit.DAYS);

    mvc.perform(get("/v1/db-audit/db-audit-log")
                    .sessionAttr("company", company)
                    .queryParam("startTime", String.valueOf(start.toEpochMilli()))
                    .queryParam("endTime", String.valueOf(end.toEpochMilli()))
            ).andExpect(status().is(500))
            .andReturn();

    String userFilter = "NOT textPayload :(\"user=operations\" OR \"user=postgres\"" +
            " OR \"user=cloudsqladmin\" OR \"user= \" OR \"user=[unknown]\")";
    String companyFilter = "textPayload :(\" " + company + ".\")";
    String additionalFilters = userFilter + " AND " + companyFilter;

    verify(gcpLoggingService, times(1)).getLogs(POSTGRES_LOGNAME, CLOUD_SQL_RESOURCE,
            additionalFilters, start, end);
  }

  @Test
  public void testGetDbAuditLogs_startAfterEnd() throws Exception {
    Instant instant = Instant.ofEpochMilli(1692057600000l);
    when(clock.instant()).thenReturn(instant);
    List<String> logEntries = new ArrayList<>();
    logEntries.add("2023-08-21 06:36:22.007 UTC [815008]: [2-1] db=postgres,user=praveen@harness STATEMENT:  select");
    logEntries.add("2023-08-17 13:28:32.829 UTC [381418]: [4-1]" +
            " db=postgres,user=nishith.patel@harness.io STATEMENT:  Select r.* from fis.cicd_jobs");
    when(gcpLoggingService.getLogs(anyString(), anyString(), anyString(), any(Instant.class),
            any(Instant.class))).thenReturn(logEntries);

    String company = "capitalone";
    Instant start = instant.minus(4, ChronoUnit.DAYS);
    Instant end = instant.minus(1, ChronoUnit.DAYS);

    mvc.perform(get("/v1/db-audit/db-audit-log")
                    .sessionAttr("company", company)
                    .queryParam("startTime", String.valueOf(end.toEpochMilli()))
                    .queryParam("endTime", String.valueOf(start.toEpochMilli()))
            ).andExpect(status().is(200))
            .andReturn();

    String userFilter = "NOT textPayload :(\"user=operations\" OR \"user=postgres\"" +
            " OR \"user=cloudsqladmin\" OR \"user= \" OR \"user=[unknown]\")";
    String companyFilter = "textPayload :(\" " + company + ".\")";
    String additionalFilters = userFilter + " AND " + companyFilter;

    verify(gcpLoggingService, times(1)).getLogs(POSTGRES_LOGNAME, CLOUD_SQL_RESOURCE,
            additionalFilters, instant.minus(1, ChronoUnit.DAYS), instant);
  }
}
