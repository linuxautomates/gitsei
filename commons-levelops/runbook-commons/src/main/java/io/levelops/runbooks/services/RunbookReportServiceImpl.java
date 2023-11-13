package io.levelops.runbooks.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
import io.levelops.commons.databases.models.database.runbooks.RunbookReportSection;
import io.levelops.commons.databases.services.RunbookReportDatabaseService;
import io.levelops.commons.databases.services.RunbookReportSectionDatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.runbooks.models.RunbookReportData;
import io.levelops.runbooks.models.RunbookReportSectionData;
import io.levelops.runbooks.models.RunbookReportSectionMetadata;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
public class RunbookReportServiceImpl implements RunbookReportService {

    private final int DEFAULT_PAGE_SIZE = 1000;
    private final String DEFAULT_REPORT_TITLE = "Untitled Report";
    private final String SECTION_TITLE_FIELD = "section_title";
    private final String bucketName;
    private final RunbookReportDatabaseService reportDatabaseService;
    private final RunbookReportSectionDatabaseService reportSectionDatabaseService;
    private final ObjectMapper objectMapper;
    private final Storage storage;
    private final int pageSize;

    /**
     * @param bucketName use @Value("${RUNBOOK_BUCKET_NAME}") for auto wiring
     */
    public RunbookReportServiceImpl(String bucketName,
                                    RunbookReportDatabaseService reportDatabaseService,
                                    RunbookReportSectionDatabaseService reportSectionDatabaseService,
                                    ObjectMapper objectMapper,
                                    Storage storage,
                                    @Nullable Integer pageSize) {
        this.bucketName = bucketName;
        this.reportDatabaseService = reportDatabaseService;
        this.reportSectionDatabaseService = reportSectionDatabaseService;
        this.objectMapper = objectMapper;
        this.storage = storage;
        this.pageSize = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
    }

    @Override
    public String getOrCreateRunbookReportId(String company, String runbookId, String runId, String nodeId, String title) throws SQLException {
        Optional<String> reportIdOpt = reportDatabaseService.insertAndReturnId(company, RunbookReport.builder()
                .runbookId(runbookId)
                .runId(runId)
                .source(nodeId)
                .title(StringUtils.firstNonBlank(title, DEFAULT_REPORT_TITLE))
                .gcsPath(generatePathPrefixForReport(company, runbookId, runId, nodeId))
                .build());
        if (reportIdOpt.isPresent()) {
            return reportIdOpt.get();
        }

        // if insert did not return, then there must be an existing report
        DbListResponse<RunbookReport> response = reportDatabaseService.filter(0, 1, company, List.of(runbookId), runId, nodeId, null);
        if (response == null || CollectionUtils.isEmpty(response.getRecords())) {
            throw new SQLException(String.format("Failed to upsert report for company=%s, runbook_id=%s, run_id=%s", company, runbookId, runbookId));
        }
        // there can only be 1
        return response.getRecords().get(0).getId();
    }

    // region STORE ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Store section data to GCS and store metadata to db.
     *
     * @return sectionId
     */
    @Override
    public String createReportSection(String tenantId, String runbookId, String runId, String nodeId, String runningNodeId, String reportTitle, String sectionTitle, String contentType, Stream<Map<String, Object>> contentStream) throws SQLException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(runbookId, "runbookId cannot be null or empty.");
        Validate.notBlank(runId, "runbookId cannot be null or empty.");
        Validate.notBlank(runningNodeId, "runningNodeId cannot be null or empty.");

        String reportId = getOrCreateRunbookReportId(tenantId, runbookId, runId, nodeId, reportTitle);

        SectionDataStorageResult sectionDataStorageResult = storeReportSectionData(tenantId, runbookId, runId, nodeId, runningNodeId, contentStream);

        RunbookReportSection section = RunbookReportSection.builder()
                .reportId(reportId)
                .source(runningNodeId)
                .gcsPath(generatePathPrefixForReportSection(tenantId, runbookId, runId, nodeId, runningNodeId))
                .pageCount(sectionDataStorageResult.getPageCount())
                .pageSize(sectionDataStorageResult.getPageSize())
                .totalCount(sectionDataStorageResult.getTotalCount())
                .title(sectionTitle)
                .metadata(ParsingUtils.toJsonObject(objectMapper, RunbookReportSectionMetadata.builder()
                        .contentType(contentType)
                        .failedPages(sectionDataStorageResult.getFailedPages())
                        .build()))
                .build();

        return reportSectionDatabaseService.insert(tenantId, section);
    }

    @Data
    @Builder
    private static final class SectionDataStorageResult {
        int pageCount;
        int pageSize;
        int totalCount;
        int failedPages;
    }

    private SectionDataStorageResult storeReportSectionData(String tenantId, String runbookId, String runId, String nodeId, String runningNodeId, Stream<Map<String, Object>> contentStream) {
        MutableInt pageNumber = new MutableInt(0);
        MutableInt totalCount = new MutableInt(0);
        MutableInt failedPages = new MutableInt(0);
        StreamUtils.forEachPage(contentStream.filter(Objects::nonNull), pageSize, list -> {
            try {
                storeOnePage(tenantId, runbookId, runId, nodeId, runningNodeId, pageNumber.getValue(), RunbookReportSectionData.builder()
                        .records(list)
                        .build());
            } catch (JsonProcessingException e) {
                log.warn("Failed to store runbook report page to GCS", e);
                failedPages.increment();
                return;
            }
            // if successful
            totalCount.add(list.size());
            pageNumber.increment();
        });
        return SectionDataStorageResult.builder()
                .pageCount(pageNumber.getValue())
                .pageSize(pageSize)
                .totalCount(totalCount.getValue())
                .failedPages(failedPages.getValue())
                .build();
    }

    private void storeOnePage(String tenantId, String runbookId, String runId, String nodeId, String runningNodeId, int pageNumber, RunbookReportSectionData data) throws JsonProcessingException {
        String path = generatePathForReportSectionPage(tenantId, runbookId, runId, nodeId, runningNodeId, pageNumber);
        byte[] bytes = objectMapper.writeValueAsBytes(data);
        uploadDataToGcs(bucketName, path, "application/json", bytes);
    }

    // endregion

    // region FETCH ////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Optional<RunbookReportData> fetchReportData(String tenantId, String runbookId, String runId, String nodeId) {
        return IterableUtils.getFirst(reportDatabaseService.filter(0, 1, tenantId, List.of(runbookId), runId, nodeId, null).getRecords())
                .map(report -> fetchReportData(tenantId, report));
    }

    @Override
    public Optional<RunbookReportData> fetchReportData(String tenantId, String reportId) throws SQLException {
        return reportDatabaseService.get(tenantId, reportId)
                .map(report -> fetchReportData(tenantId, report));
    }

    private RunbookReportData fetchReportData(String tenantId, RunbookReport report) {
        // TODO paginate
        Set<String> columns = new HashSet<>();
        List<String> sectionTitles = new ArrayList<>();
        List<Map<String, Object>> records = reportSectionDatabaseService.stream(tenantId, null, report.getId())
                .peek(section -> sectionTitles.add(section.getTitle()))
                .flatMap(section -> fetchReportSectionData(tenantId, section.getId())
                        .peek(row -> columns.addAll(row.keySet()))
                        .map(row -> MapUtils.append(row, SECTION_TITLE_FIELD, section.getTitle())))
                .collect(Collectors.toList());

        ArrayList<String> columnList = Lists.newArrayList(columns);
        columnList.add(0, SECTION_TITLE_FIELD);

        return RunbookReportData.builder()
                .report(report)
                .records(records)
                .columns(columnList)
                .sectionTitles(sectionTitles)
                .build();
    }

    private Stream<Map<String, Object>> fetchReportSectionData(String tenantId, String sectionId) {
        RunbookReportSection section;
        try {
            Optional<RunbookReportSection> sectionOpt = reportSectionDatabaseService.get(tenantId, sectionId);
            if (sectionOpt.isEmpty()) {
                return Stream.empty();
            }
            section = sectionOpt.get();
        } catch (SQLException e) {
            log.warn("Failed to retrieve report section with id={} for tenant={}", sectionId, tenantId);
            return Stream.empty();
        }
        int pageCount = MoreObjects.firstNonNull(section.getPageCount(), 0);
        if (pageCount <= 0 || Strings.isEmpty(section.getGcsPath())) {
            return Stream.empty();
        }
        String sectionPath = StringUtils.appendIfMissing(section.getGcsPath(), "/");
        return IntStream.range(0, pageCount)
                .mapToObj(pageNumber -> {
                    String pagePath = generatePathForReportSectionPage(sectionPath, pageNumber);
                    try {
                        return fetchOnePage(pagePath);
                    } catch (IOException e) {
                        log.warn("Failed to retrieve report section data for tenant={} at path={}", tenantId, pagePath);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(RunbookReportSectionData::getRecords)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream);

    }

    private RunbookReportSectionData fetchOnePage(String path) throws IOException {
        byte[] bytes = downloadDataFromGcs(bucketName, path);
        return objectMapper.readValue(bytes, RunbookReportSectionData.class);
    }

    // endregion

    // region DELETE ////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void deleteReport(String company, String reportId) throws SQLException {
        Optional<RunbookReport> reportOpt = reportDatabaseService.get(company, reportId);
        if (reportOpt.isEmpty()) {
            return;
        }

        deleteReportData(company, reportOpt.get());

        List<String> sectionIds = reportSectionDatabaseService.stream(company, null, reportId)
                .map(RunbookReportSection::getId)
                .collect(Collectors.toList());
        sectionIds.forEach(id -> {
            try {
                reportSectionDatabaseService.delete(company, id);
            } catch (SQLException e) {
                log.warn("Failed to delete runbook report section with id=" + id, e);
            }
        });

        reportDatabaseService.delete(company, reportId);
    }

    private void deleteReportData(String company, RunbookReport report) {
        String gcsPath = report.getGcsPath();
        if (Strings.isEmpty(gcsPath)) {
            return;
        }
        // sanity check
        if (!gcsPath.contains(company + "/runbook_reports/rb-" + report.getRunbookId())) {
            log.warn("Not deleting GCS data that is not prefixed by {company}/runbook_reports");
            return;
        }
        listBlobs(gcsPath, true)
                .forEach(blob -> {
                    // sanity check
                    if (!blob.getName().contains(company + "/runbook_reports/rb-" + report.getRunbookId())) {
                        return;
                    }
                    try {
                        deleteDataFromGcs(blob.getName());
                    } catch (Exception e) {
                        log.warn("Failed to delete data from GCS at path={}", gcsPath);
                    }
                });
    }

    // end region

    private static String generatePathForReportSectionPage(String tenantId, String runbookId, String runId, String nodeId, String runningNodeId, int pageNumber) {
        return generatePathForReportSectionPage(generatePathPrefixForReportSection(tenantId, runbookId, runId, nodeId, runningNodeId), pageNumber);
    }

    private static String generatePathForReportSectionPage(String pathPrefix, int pageNumber) {
        return pathPrefix + String.format("%d.json", pageNumber);
    }

    private static String generatePathPrefixForReportSection(String tenantId, String runbookId, String runId, String nodeId, String runningNodeId) {
        return generatePathPrefixForReport(tenantId, runbookId, runId, nodeId) + String.format("running-node-%s/", runningNodeId);
    }

    private static String generatePathPrefixForReport(String tenantId, String runbookId, String runId, String nodeId) {
        // ADDED '/' AT THE BEGINNING FOR CONSISTENCY BUT IT SHOULD BE REMOVED!!!
        return String.format("/%s/runbook_reports/rb-%s/run-%s/node-%s/", tenantId, runbookId, runId, nodeId);
    }

    //region GCS utils
    private Iterable<Blob> listBlobs(String pathPrefix, boolean recursive) {
        ArrayList<BlobListOption> listOptions = new ArrayList<>();
        listOptions.add(BlobListOption.prefix(pathPrefix));
        if (!recursive) {
            listOptions.add(BlobListOption.currentDirectory());
        }
        return storage.list(bucketName, listOptions.toArray(BlobListOption[]::new)).iterateAll();
    }

    private byte[] downloadDataFromGcs(String bucketName, String path) {
        log.info("Downloading content from {}:{}", bucketName, path);
        Blob blob = storage.get(BlobId.of(bucketName, path));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toByteArray();
    }

    private Blob uploadDataToGcs(String bucketName, String path, String contentType, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        log.info("Uploading '{}' content to {}:{}", contentType, bucketName, path);
        return storage.create(blobInfo, content);
    }

    private void deleteDataFromGcs(String path) {
        storage.delete(bucketName, path);
    }
    // endregion

}
