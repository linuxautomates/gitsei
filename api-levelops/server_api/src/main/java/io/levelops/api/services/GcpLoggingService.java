package io.levelops.api.services;

import com.google.api.gax.paging.Page;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
@Service
public class GcpLoggingService {

  /**
   * Assumes that GOOGLE_APPLICATION_CREDENTIALS env variable has been set to grant permissions for gcp logs.
   * @param logName logName in gcp ex: cloudsql.googleapis.com%2Fpostgres.log
   * @param resourceName Gcp resource name
   * @param additionalFilters any additional filters formatted with an AND or OR
   * @param startTime startTimestamp to filter logs
   * @param endTime endTimestamp to filter logs
   * @return List of Strings containing the log lines.
   */
  public List<String> getLogs(String logName,
                              String resourceName, String additionalFilters, Instant startTime, Instant endTime) {
    try {
      Logging logging = LoggingOptions.getDefaultInstance().getService();

      Date startDate = Date.from(startTime);
      Date endDate = Date.from(endTime);
      DateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

      String timestampFilter = "timestamp >= \""+ rfc3339.format(startDate) + "\" AND timestamp < \""
              + rfc3339.format(endDate) + "\"";

      String logFilter =
              "logName=projects/"
                      + logging.getOptions().getProjectId()
                      + "/logs/" + logName
                      + " AND resource.type=\""+ resourceName + "\""
                      + " AND " + additionalFilters
                      + " AND " + timestampFilter;

      Page<LogEntry> entries = logging.listLogEntries(Logging.EntryListOption.filter(logFilter));
      List<String> payloadLogLines = new ArrayList<>();

      while (entries != null) {
        for (LogEntry logEntry : entries.iterateAll()) {
          payloadLogLines.add((String) logEntry.getPayload().getData());
        }
        entries = entries.getNextPage();
      }
      //TODO: This might lead to OOM if the request time range is too high, come back and paginate.
      return payloadLogLines;
    } catch (Exception ex) {
      throw ex;
    }
  }

}
