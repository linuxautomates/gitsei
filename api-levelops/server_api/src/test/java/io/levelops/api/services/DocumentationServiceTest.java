package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gcs.models.Category;
import io.levelops.integrations.gcs.models.ReportDocumentation;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.services.GcsStorageService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unused")
public class DocumentationServiceTest {

    private final GcsStorageService gcsStorageService = mock(GcsStorageService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentationService documentationService = new DocumentationService(gcsStorageService,
            objectMapper, 0, 0, 0);

    private String report;
    private String reportList;
    private String categoryList;

    @Before
    public void setup() throws IOException {
        String categoryListResourcePath = "report_docs/category_list.json";
        categoryList = ResourceUtils.getResourceAsString(categoryListResourcePath);
        String reportListResourcePath = "report_docs/report_list.json";
        reportList = ResourceUtils.getResourceAsString(reportListResourcePath);
        String reportResourcePath = "report_docs/report.json";
        report = ResourceUtils.getResourceAsString(reportResourcePath);
    }

    @Test
    public void testGetReportDoc() throws IOException {
        Mockito.when(gcsStorageService.read(eq("reports/reports.json"))).thenReturn(StorageData.builder()
                .content(reportList.getBytes(StandardCharsets.UTF_8))
                .build());
        ReportDocumentation reportDoc1 = documentationService.getReportDoc("Jira_Bounce_Report");
        ReportDocumentation reportDoc2 = documentationService.getReportDoc("");
        Assert.assertNull(reportDoc2);
        assertThat(reportDoc1.getId()).isEqualTo("Jira_Bounce_Report");
        assertThat(reportDoc1.getReports().stream()
                .filter(reportDocument -> reportDocument.equals("Jira_Bounce_Report")).findFirst().get());
        assertThat(reportDoc1.getApplications().size()).isEqualTo(1);
        assertThat(reportDoc1.getReports().size()).isEqualTo(3);
        assertThat(reportDoc1.getVariants().size()).isEqualTo(1);
        assertThat(reportDoc1.getCategories().size()).isEqualTo(1);
        assertThat(reportDoc1.getRelatedReports().size()).isEqualTo(1);
        assertThat(reportDoc1.getImageUrl()).isEqualTo("https://testapi1.levelops.io/v1/docs/reports/images/jira-bounce-report.png");


    }

    @Test
    public void testGetReportsLists() throws IOException {
        Mockito.when(gcsStorageService.read(eq("reports/reports.json"))).thenReturn(StorageData.builder()
                .content(reportList.getBytes(StandardCharsets.UTF_8))
                .build());
        DbListResponse<ReportDocumentation> reports = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("applications", List.of("JUnit"), "categories", List.of("Quality"))).build());
        assertThat(reports.getCount()).isEqualTo(1);
        assertThat(reports.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Junit_Test_Report")).findFirst().get());
        assertThat(reports.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports1 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("applications", List.of("JUnit"), "categories", List.of())).build());
        assertThat(reports1.getCount()).isEqualTo(1);
        assertThat(reports1.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Junit_Test_Report")).findFirst().get());
        assertThat(reports1.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports2 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("applications", List.of(), "categories", List.of("Quality"))).build());
        assertThat(reports2.getCount()).isEqualTo(2);
        assertThat(reports2.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Junit_Test_Report")).findFirst().get());
        assertThat(reports2.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports3 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("reports", List.of("Junit_Test_Report"), "applications", List.of("JUnit"))).build());
        assertThat(reports3.getCount()).isEqualTo(1);
        assertThat(reports3.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Junit_Test_Report")).findFirst().get());
        assertThat(reports3.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports4 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("reports", List.of("Zendesk_Hygiene_Report"), "categories", List.of("Customer Support"))).build());
        assertThat(reports4.getCount()).isEqualTo(1);
        assertThat(reports4.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Zendesk_Hygiene_Report")).findFirst().get());
        assertThat(reports4.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports5 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("reports", List.of("Zendesk_Hygiene_Report"), "categories", List.of())).build());
        assertThat(reports5.getCount()).isEqualTo(1);
        assertThat(reports5.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Zendesk_Hygiene_Report")).findFirst().get());
        assertThat(reports5.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports6 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of("Jira_Bounce_Report"), "reports", List.of())).build());
        assertThat(reports6.getCount()).isEqualTo(1);
        assertThat(reports6.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports6.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports7 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("reports", List.of("Jira_Hops_Report"), "related-reports", List.of())).build());
        assertThat(reports7.getCount()).isEqualTo(1);
        assertThat(reports7.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports7.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports8 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of("Jira_Bounce_Report"), "applications", List.of("Jira"))).build());
        assertThat(reports8.getCount()).isEqualTo(1);
        assertThat(reports8.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports8.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports9 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of("Jira_Bounce_Report"), "applications", List.of())).build());
        assertThat(reports9.getCount()).isEqualTo(1);
        assertThat(reports9.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports9.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports10 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of(), "applications", List.of("Jira"))).build());
        assertThat(reports10.getCount()).isEqualTo(3);
        assertThat(reports10.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports10.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports11 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of("Jira_Bounce_Report"), "categories", List.of())).build());
        assertThat(reports11.getCount()).isEqualTo(1);
        assertThat(reports11.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports11.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports12 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of(), "categories", List.of("Velocity"))).build());
        assertThat(reports12.getCount()).isEqualTo(3);
        assertThat(reports12.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports12.getTotalCount()).isEqualTo(6);

        DbListResponse<ReportDocumentation> reports13 = documentationService.getReportDocumentsList(DefaultListRequest.builder()
                .filter(Map.of("related-reports", List.of("Jira_Bounce_Report"), "categories", List.of("Velocity"))).build());
        assertThat(reports13.getCount()).isEqualTo(1);
        assertThat(reports13.getRecords().stream()
                .filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Hops_Report")).findFirst().get());
        assertThat(reports13.getTotalCount()).isEqualTo(6);
    }

    @Test
    public void testGetCategoriesLists() throws IOException {
        Mockito.when(gcsStorageService.read(eq("reports/categories.json"))).thenReturn(StorageData.builder()
                .content(categoryList.getBytes(StandardCharsets.UTF_8))
                .build());
        DbListResponse<Category> category = documentationService.getCategoriesList(DefaultListRequest.builder()
                .filter(Map.of("applications", List.of())).build());
        assertThat(category.getCount()).isEqualTo(1);
        assertThat(category.getTotalCount()).isEqualTo(1);
        assertThat(category.getRecords().stream().filter(report -> report.getName().equals("Customer Insights")).findFirst().get());
        assertThat(category.getRecords().stream().filter(report -> report.getReportDocumentations().size() == 2).findFirst().get());

        DbListResponse<Category> category1 = documentationService.getCategoriesList(DefaultListRequest.builder()
                .filter(Map.of("applications", List.of("Jira"))).build());
        assertThat(category1.getCount()).isEqualTo(1);
        assertThat(category1.getTotalCount()).isEqualTo(1);
        assertThat(category1.getRecords().stream().findFirst().get().getReportDocumentations()
                .stream().filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Salesforce_Report")).findFirst().get());
        assertThat(category1.getRecords().stream().filter(report -> report.getReportDocumentations().size() == 1).findFirst().get());

        DbListResponse<Category> category2 = documentationService.getCategoriesList(DefaultListRequest.builder()
                .filter(Map.of("applications", List.of("Jira", "Salesforce"))).build());
        assertThat(category2.getCount()).isEqualTo(1);
        assertThat(category2.getRecords().stream().findFirst().get().getReportDocumentations()
                .stream().filter(reportDocumentation -> reportDocumentation.getId().equals("Jira_Salesforce_Report")).findFirst().get());
        assertThat(category2.getRecords().stream().filter(report -> report.getReportDocumentations().size() == 2).findFirst().get());
    }
}
