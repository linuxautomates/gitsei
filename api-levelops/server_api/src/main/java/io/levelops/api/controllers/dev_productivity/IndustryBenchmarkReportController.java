package io.levelops.api.controllers.dev_productivity;

import io.levelops.api.services.dev_productivity.IndustryBenchmarkReportService;
import io.levelops.commons.databases.models.database.dev_productivity.IndustryDevProductivityReport;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/dev_productivity/industry/report")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
public class IndustryBenchmarkReportController {
    private IndustryBenchmarkReportService industryBenchmarkReportService;

    @Autowired
    public IndustryBenchmarkReportController(IndustryBenchmarkReportService industryBenchmarkReportService) {
        this.industryBenchmarkReportService = industryBenchmarkReportService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> create(@RequestBody IndustryDevProductivityReport industryDevProductivityReport,
                                                                          @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", industryBenchmarkReportService.create(company, industryDevProductivityReport))));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{reportid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> delete(@PathVariable("reportid") String reportId,
                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            industryBenchmarkReportService.delete(company, reportId);
            return ResponseEntity.ok().build();
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{reportid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> update(@RequestBody IndustryDevProductivityReport industryDevProductivityReport,
                                                                          @PathVariable("reportid") String reportId,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            final IndustryDevProductivityReport sanitizedIndustryDevProductivityReport = (industryDevProductivityReport.getId() != null) ? industryDevProductivityReport : industryDevProductivityReport.toBuilder().id(UUID.fromString(reportId)).build();
            if(!reportId.equals(sanitizedIndustryDevProductivityReport.getId().toString())){
                throw new BadRequestException("For customer " + company + " report id " + sanitizedIndustryDevProductivityReport.getId().toString() + " failed to update report!");
            }
            return ResponseEntity.ok(Map.of("id", industryBenchmarkReportService.update(company, sanitizedIndustryDevProductivityReport)));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{reportid}", produces = "application/json")
    public DeferredResult<ResponseEntity<IndustryDevProductivityReport>> getReportData(@PathVariable("reportid") String reportId,
                                                                  @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            IndustryDevProductivityReport industryDevProductivityReport = industryBenchmarkReportService.get(company, reportId)
                    .orElseThrow(() -> new NotFoundException("Could not find report for Industry bench mark with id=" + reportId));
            return ResponseEntity.ok(industryDevProductivityReport);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<IndustryDevProductivityReport>>> reportList(@RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
                                                                                                @SessionAttribute(name = "company") String company,
                                                                                                @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<IndustryDevProductivityReport> internalApiResponse = PaginatedResponse.of(filter.getPage(), filter.getPageSize(), industryBenchmarkReportService.listByFilter("_levelops", filter));
            log.info("internalApiResponse = {}", internalApiResponse);
            return ResponseEntity.ok(internalApiResponse);
        });
    }

}
