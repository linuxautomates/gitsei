package io.levelops.api.controllers.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.paging.Page;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.EntryListOption;
import com.google.cloud.logging.LoggingOptions;
import io.levelops.api.services.GcpLoggingService;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.models.ListResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@Log4j2
@RequestMapping("/v1/db-audit")
@SuppressWarnings("unused")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
public class DatabaseAuditController {

  static final String USER_REGEX = "user=[^\\s]* ";
  static final String TIMESTAMP_REGEX = "[^UTC]+";

  public static final String POSTGRES_LOGNAME = "cloudsql.googleapis.com%2Fpostgres.log";
  public static final String CLOUD_SQL_RESOURCE = "cloudsql_database";

  private final ActivityLogService activityLogService;

  private final Pattern userPattern;
  private final Pattern timestampPattern;
  private final GcpLoggingService gcpLoggingService;
  private Clock clock;


  @Autowired
  public DatabaseAuditController(ActivityLogService activityLogService,
                                 GcpLoggingService gcpLoggingService, Clock clock) {
    this.activityLogService = activityLogService;
    this.gcpLoggingService = gcpLoggingService;
    userPattern = Pattern.compile(USER_REGEX);
    timestampPattern = Pattern.compile(TIMESTAMP_REGEX);
    //using clock makes it easier to write tests
    this.clock = clock;
  }

  @GetMapping("/db-audit-log")
  public ResponseEntity<ListResponse<String>> getDbAuditLogForTenant(
          @RequestParam(value = "startTime", required = false, defaultValue = "") Long startTime,
          @RequestParam(value = "endTime", required = false, defaultValue = "") Long endTime,
          @SessionAttribute(name = "company") String company) {

    try (Logging logging = LoggingOptions.getDefaultInstance().getService()) {


      Instant now = clock.instant();
      //validate start/end Times

      //if start and end times are null, we will default to the last one day.
      Instant start = validateTime(startTime, now.minus(1, ChronoUnit.DAYS));
      Instant end = validateTime(endTime, now);


      if (start.isAfter(end)) {
        end = now;
        start = end.minus(1, ChronoUnit.DAYS);
      } else if (end.toEpochMilli() - start.toEpochMilli() > TimeUnit.DAYS.toMillis(7)) {
        //if the length is more than 7 days, we will restrict the end time to be start+7 days
        end = start.plus(7, ChronoUnit.DAYS);
      }

      String userFilter = "NOT textPayload :(\"user=operations\" OR \"user=postgres\"" +
              " OR \"user=cloudsqladmin\" OR \"user= \" OR \"user=[unknown]\")";

      String companyFilter = "textPayload :(\" " + company + ".\")";


      String additionalFilters = userFilter + " AND " + companyFilter;

      List<String> logLines = gcpLoggingService.getLogs(POSTGRES_LOGNAME, CLOUD_SQL_RESOURCE, additionalFilters, start, end);
      List<String> dbAuditLines = new ArrayList<>();
      logLines.forEach(logLine -> {
        //log payload looks like -
        // 2023-08-21 06:36:22.007 UTC [815008]: [2-1] db=postgres,user=praveen@harness STATEMENT:  select ...
        Matcher userMatcher = userPattern.matcher(logLine);
        Matcher timestampMatcher = timestampPattern.matcher(logLine);

        while(userMatcher.find() && timestampMatcher.find()) {
          String user = userMatcher.group();
          String time = timestampMatcher.group();

          dbAuditLines.add(user + " accessed the database at " + time + " UTC");
        }
      });

      return ResponseEntity.ok().body(ListResponse.of(dbAuditLines));
    } catch (Exception ex) {
      log.error("Exception while fetching DB audit log for {} between {} and {}",
              company, startTime, endTime, ex);
      return ResponseEntity.internalServerError().build();
    }
  }

  private Instant validateTime(Long timestamp, Instant defaultValue) {
    if (timestamp == null) {
      timestamp = defaultValue.toEpochMilli();
    }
    return Instant.ofEpochMilli(timestamp);
  }
}
