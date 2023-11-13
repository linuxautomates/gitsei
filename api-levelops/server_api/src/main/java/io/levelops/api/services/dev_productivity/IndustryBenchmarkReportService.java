package io.levelops.api.services.dev_productivity;

import io.levelops.api.model.dev_productivity.IndustryDevProductivityFixedIntervalFilter;
import io.levelops.commons.databases.models.database.dev_productivity.IndustryDevProductivityReport;
import io.levelops.commons.databases.services.dev_productivity.IndustryDevProductivityReportDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class IndustryBenchmarkReportService {
    private static final Long LOWER_LIMIT = TimeUnit.DAYS.toSeconds(10);
    private static final Long UPPER_LIMIT = TimeUnit.DAYS.toSeconds(30);
    private static final EnumSet<IntegrationType> ISSUE_MANAGEMENT_INTEGRATION_TYPES = IntegrationType.getIssueManagementIntegrationTypes();

    private final IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService;

    @Autowired
    public IndustryBenchmarkReportService(IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService) {
        this.industryDevProductivityReportDatabaseService = industryDevProductivityReportDatabaseService;
    }

    public String create(final String company, final IndustryDevProductivityReport industryDevProductivityReport) throws SQLException, BadRequestException {
        try {
            return industryDevProductivityReportDatabaseService.insert(company, industryDevProductivityReport);
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage());
            if(e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new BadRequestException("The value for industry dev productivity report already exists");
            } else {
                throw new BadRequestException(e);
            }
        }
    }

    public String update(final String company, final IndustryDevProductivityReport industryDevProductivityReport) throws SQLException, BadRequestException {
        Boolean updateResult = null;
        try {
            updateResult = industryDevProductivityReportDatabaseService.update(company, industryDevProductivityReport);
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage());
            if(e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new BadRequestException("A value for industry dev productivity already exists");
            } else {
                throw new BadRequestException(e);
            }
        }
        if(!Boolean.TRUE.equals(updateResult)) {
            throw new RuntimeException("For customer " + company + " report id " + industryDevProductivityReport.getId().toString() + " failed to update config!");
        }
        return industryDevProductivityReport.getId().toString();
    }

    public Boolean delete(final String company, final String id) throws SQLException {
        return industryDevProductivityReportDatabaseService.delete(company, id);
    }

    public Optional<IndustryDevProductivityReport> get(final String company, final String id) throws SQLException {
        Optional<IndustryDevProductivityReport> industryDevProductivityReport = industryDevProductivityReportDatabaseService.get(company, id);
        if(industryDevProductivityReport.isEmpty()) {
            return Optional.empty();
        }
        return industryDevProductivityReport;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DbListResponse<IndustryDevProductivityReport> listByFilter(String company, DefaultListRequest filter) throws SQLException, BadRequestException {
        IndustryDevProductivityFixedIntervalFilter industryReportFilter = IndustryDevProductivityFixedIntervalFilter.fromListRequest(filter);
        DbListResponse<IndustryDevProductivityReport> dbListResponse = industryDevProductivityReportDatabaseService.listByFilter(company,filter.getPage(),filter.getPageSize(),null,List.of(industryReportFilter.getReportInterval()), industryReportFilter.getSort());
        log.info("dbListResponse totalCount {}, count {}", dbListResponse.getTotalCount(), dbListResponse.getCount());
        return  dbListResponse;
    }

}
