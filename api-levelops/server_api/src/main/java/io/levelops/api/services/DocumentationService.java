package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.integrations.gcs.models.Category;
import io.levelops.integrations.gcs.models.ReportDocumentation;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.services.GcsStorageService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
@SuppressWarnings("unused")
public class DocumentationService {

    private static final String REPORTS = "reports";
    private static final String CATEGORIES = "categories";
    private static final String FILE_PREFIX = "reports/";
    private static final String FILE_SUFFIX = ".json";
    private static final String IMAGE_PREFIX = "images/";

    private final GcsStorageService gcsStorageService;
    private final ObjectMapper objectMapper;
    private final LoadingCache<String, StorageData> cache;

    @Autowired
    public DocumentationService(ObjectMapper objectMapper,
                                @Value("${DOCUMENTATION_BUCKET:dev-ingestion-levelops}") String bucketName,
                                @Value("${DOCUMENTATION_PREFIX:docs}") String pathPrefix,
                                @Value("${docs.cache.maxSize:250}") int maxSize,
                                @Value("${docs.cache.expireAccess:60}") int expireAccess,
                                @Value("${docs.cache.expireWrite:60}") int expireWrite) {
        this(new GcsStorageService(bucketName, pathPrefix), objectMapper, maxSize, expireAccess, expireWrite);
    }

    DocumentationService(GcsStorageService gcsStorageService, ObjectMapper objectMapper,
                         int maxSize, int expireAccess, int expireWrite) {
        this.gcsStorageService = gcsStorageService;
        this.objectMapper = objectMapper;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expireAccess, TimeUnit.MINUTES)
                .expireAfterWrite(expireWrite, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public StorageData load(@NotNull String reportName) throws IOException {
                        return gcsStorageService.read(reportName);
                    }
                });
    }

    public byte[] getImage(String imageName) throws IOException {
        StorageData gcsData;
        try {
            gcsData = cache.get(FILE_PREFIX + IMAGE_PREFIX + imageName);
        } catch (Exception e) {
            log.warn("Image {} not found ", imageName);
            throw new FileNotFoundException("Image " + imageName + " not found");
        }
        log.debug("Image {} fetched successfully", imageName);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(gcsData.getContent()));
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArray);
        return byteArray.toByteArray();
    }

    public ReportDocumentation getReportDoc(String reportName) throws IOException {
        DefaultListRequest filter = DefaultListRequest.builder()
                .filter(Map.of("reports", List.of(reportName)))
                .build();
        try {
            DbListResponse<ReportDocumentation> reportDocumentations = getReportDocumentsList(filter);
            log.debug("File {} fetched successfully", reportName);
            return reportDocumentations.getRecords().stream().findFirst().orElse(null);
        } catch (FileNotFoundException e) {
            log.warn("File {} not found ", reportName);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<ReportDocumentation> getReportDocumentsList(DefaultListRequest filter) throws IOException {
        StorageData gcsData;
        try {
            gcsData = cache.get(FILE_PREFIX + REPORTS + FILE_SUFFIX);
        } catch (ExecutionException e) {
            log.warn("File {} not found ", REPORTS);
            throw new FileNotFoundException("File " + REPORTS + " not found");
        }
        log.debug("File {} fetched successfully", REPORTS);
        List<ReportDocumentation> reports = Arrays.asList(objectMapper.readValue(gcsData.getContent(), ReportDocumentation[].class));
        final List<String> filterApplications = new ArrayList<>();
        if (filter.getFilter().containsKey("applications"))
            filterApplications.addAll((List<String>) filter.getFilter().get("applications"));
        final List<String> filterReports = new ArrayList<>();
        if (filter.getFilter().containsKey("reports"))
            filterReports.addAll((List<String>) filter.getFilter().get("reports"));
        final List<String> filterRelatedReports = new ArrayList<>();
        if (filter.getFilter().containsKey("related-reports"))
            filterRelatedReports.addAll((List<String>) filter.getFilter().get("related-reports"));
        final List<String> filterReportCategories = new ArrayList<>();
        if (filter.getFilter().containsKey("categories"))
            filterReportCategories.addAll((List<String>) filter.getFilter().get("categories"));
        Stream<ReportDocumentation> reportsStream = reports.stream();
        if (!filterRelatedReports.isEmpty()) {
            reportsStream = reportsStream
                    .filter(reportDocument -> reportDocument.getRelatedReports() != null &&
                            CollectionUtils.containsAny(reportDocument.getRelatedReports(), filterRelatedReports));
        }
        if (!filterApplications.isEmpty()) {
            reportsStream = reportsStream
                    .filter(reportDocument -> reportDocument.getApplications() != null &&
                            CollectionUtils.containsAny(reportDocument.getApplications(), filterApplications));
        }
        if (!filterReports.isEmpty()) {
            reportsStream = reportsStream
                    .filter(reportDocument -> reportDocument.getReports() != null &&
                            CollectionUtils.containsAny(reportDocument.getReports(), filterReports));
        }
        if (!filterReportCategories.isEmpty()) {
            reportsStream = reportsStream
                    .filter(reportDocument -> reportDocument.getCategories() != null &&
                            CollectionUtils.containsAny(reportDocument.getCategories(), filterReportCategories));
        }
        List<ReportDocumentation> reportsList = reportsStream.collect(Collectors.toList());
        return DbListResponse.of(reportsList, reports.size());
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<Category> getCategoriesList(DefaultListRequest filter) throws IOException {
        StorageData gcsData;
        try {
            gcsData = cache.get(FILE_PREFIX + CATEGORIES + FILE_SUFFIX);
        } catch (ExecutionException e) {
            log.warn("File {} not found ", CATEGORIES);
            throw new FileNotFoundException("File " + CATEGORIES + " not found");
        }
        log.debug("File {} fetched successfully", CATEGORIES);
        List<Category> categories = Arrays.asList(objectMapper.readValue(gcsData.getContent(), Category[].class));
        List<String> filterValue = (List<String>) filter.getFilter().get("applications");
        if (filterValue == null || filterValue.isEmpty())
            return DbListResponse.of(categories, categories.size());
        List<Category> categoryList = categories.stream()
                .map(category -> {
                    Category.CategoryBuilder builder = category.toBuilder();
                    List<ReportDocumentation> existingReports = category.getReportDocumentations();
                    List<ReportDocumentation> resultList = existingReports.stream()
                            .filter(report -> CollectionUtils.containsAny(report.getApplications(), filterValue))
                            .collect(Collectors.toList());
                    return builder.reportDocumentations(resultList).build();
                }).collect(Collectors.toList());
        categoryList = categoryList.stream().filter(category -> !category.getReportDocumentations().isEmpty()).collect(Collectors.toList());
        return DbListResponse.of(categoryList, categories.size());
    }
}
