package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.AiReport;
import io.levelops.commons.databases.services.AiReportDatabaseService;
import io.levelops.commons.databases.services.AiReportDatabaseService.AiReportFilter;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;

@RestController
@RequestMapping("/v1/ai_reports")
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
@SuppressWarnings("unused")
public class AiReportController {

    private static final String TRELLIS_ALL_OUS_AI_REPORT_TYPE = "trellis_all_ous";
    private static final String TRELLIS_OU_CATEGORY_AI_REPORT_TYPE = "trellis_ou_category";
    private final AiReportDatabaseService aiReportDatabaseService;

    @Autowired
    public AiReportController(AiReportDatabaseService aiReportDatabaseService) {

        this.aiReportDatabaseService = aiReportDatabaseService;
    }

    @GetMapping(path = "/report", produces = "application/json")
    public DeferredResult<ResponseEntity<AiReport>> getReport(@SessionAttribute("company") final String company,
                                                              @RequestParam String type,
                                                              @RequestParam String key) {
        return SpringUtils.deferResponse(() -> {
            AiReport response = aiReportDatabaseService.get(company, type, key)
                    .orElseThrow(() -> new NotFoundException("Could not find AI Report of type=" + type + " for key=" + key));

            return ResponseEntity.ok(response);
        });
    }

    @GetMapping(path = "/trellis_all_ous_report", produces = "application/json")
    public DeferredResult<ResponseEntity<AiReport>> getAllOusTrellisReport(@SessionAttribute("company") final String company) {
        return getReport(company, TRELLIS_ALL_OUS_AI_REPORT_TYPE, company);
    }

    @GetMapping(path = "/trellis_ou_category_report", produces = "application/json")
    public DeferredResult<ResponseEntity<AiReport>> getOuCategoryTrellisReport(@SessionAttribute("company") final String company,
                                                                               @RequestParam(name = "ou_category_id") String ouCategoryId) {
        return getReport(company, TRELLIS_OU_CATEGORY_AI_REPORT_TYPE, ouCategoryId);
    }

    @PostMapping(path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AiReport>>> list(@SessionAttribute("company") final String company,
                                                                            @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> types = getListOrDefault(filter.getFilter(), "types");
            List<String> keys = getListOrDefault(filter.getFilter(), "keys");

            AiReportFilter aiReportFilter = AiReportFilter.builder()
                    .types(types)
                    .keys(keys)
                    .build();

            DbListResponse<AiReport> response = aiReportDatabaseService.filter(filter.getPage(), filter.getPageSize(), company, aiReportFilter);

            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), response));
        });
    }

}