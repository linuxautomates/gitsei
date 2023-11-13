package io.levelops.runbooks.services;

import io.levelops.runbooks.models.RunbookReportData;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface RunbookReportService {


    String getOrCreateRunbookReportId(String company, String runbookId, String runId, String nodeId, String title) throws SQLException;

    String createReportSection(String tenantId, String runbookId, String runId, String nodeId, String runningNodeId, String reportTitle, String sectionTitle, String contentType, Stream<Map<String, Object>> contentStream) throws SQLException;

    Optional<RunbookReportData> fetchReportData(String tenantId, String runbookId, String runId, String nodeId);

    Optional<RunbookReportData> fetchReportData(String tenantId, String reportId) throws SQLException;

    void deleteReport(String company, String reportId) throws SQLException;

}
