package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.plugins.MsTmtVulnerability;
import io.levelops.commons.databases.services.MsTMTDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/v1/mtmt/issues")
public class MsThreatModelingToolIssuesController {

    private final MsTMTDatabaseService msTmtService;

    @Autowired
    public MsThreatModelingToolIssuesController(final MsTMTDatabaseService msTmtService) {
        this.msTmtService = msTmtService;
    }

    @PostMapping("/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<MsTmtVulnerability>>> listIssues(@SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(body.getPage(), body.getPageSize(),
                msTmtService.filter(company, QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize()))));
    }

    @PostMapping("/aggs")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> aggregateIssues(@SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(body.getStacks())) {
                var acrossResults = msTmtService.aggregate(company, body.getAcross(), QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize());
                return ResponseEntity.ok(PaginatedResponse.of(body.getPage(), body.getPageSize(), acrossResults.getTotalCount(), acrossResults.getRecords()));
            }
            List<Map<String, Object>> records = new ArrayList<>();
            var totalCount = new AtomicInteger(0);
            var acrossResults = msTmtService.getValues(company, body.getAcross(), QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize());
            acrossResults.getRecords().forEach(filter -> {
                var key = filter.get("key") != null ? filter.get("key").toString() : null;
                var value = filter.getOrDefault("value", key).toString();
                var stacks = new ArrayList<>();
                body.getStacks().forEach(pivot -> {
                    var tmp = msTmtService.aggregate(company, pivot,
                            QueryFilter.fromRequestFilters(body.getFilter()).toBuilder().strictMatch(body.getAcross(), key).build(), body.getPage(),
                            body.getPageSize());
                    if (tmp == null || tmp.getCount() < 1) {
                        return;
                    }
                    stacks.addAll(tmp.getRecords());
                    if (totalCount.get() < tmp.getTotalCount()) {
                        totalCount.set(tmp.getTotalCount());
                    }
                });
                records.add(Map.of("key", value, "stacks", stacks));
            });
            return ResponseEntity.ok(PaginatedResponse.of(body.getPage(), body.getPageSize(), totalCount.get(), records));
        });
    }

    @PostMapping("/aggs/latest")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> aggregateIssuesOfLatestReport(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body) {
        Map<String, Object> filter = body.getFilter();

        // pick latest
        filter.put("n_last_reports", 1);

        // remove ingested_at filter
        filter.remove("ingested_at");

        // ensure single project in filter
        Object projects = filter.get("project");
        if (projects instanceof Collection) {
            var collection = (Collection<?>) projects;
            if (collection.size() > 0) {
                projects = List.of(collection.iterator().next());
            }
        }
        filter.put("project", projects);

        body = body.toBuilder().filter(filter).build();
        return aggregateIssues(company, body);
    }

    @PostMapping("/values")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> getValues(@SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body) {
        return SpringUtils.deferResponse(() -> {
            List<Map<String, Object>> records = new ArrayList<>();
            var totalCount = new AtomicInteger(0);
            body.getFields().forEach(field -> {
                var values = msTmtService.getValues(company, field, QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize());
                if (values == null || values.getTotalCount() < 1) {
                    return;
                }
                records.add(Map.of(field, values.getRecords()));
                if (totalCount.get() < values.getTotalCount()) {
                    totalCount.set(values.getTotalCount());
                }
            });
            return ResponseEntity.ok(PaginatedResponse.of(body.getPage(), body.getPageSize(), totalCount.get(), records));
        });
    }
}
