package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.models.JenkinsMonitoringAggData;
import io.levelops.aggregations.models.JenkinsMonitoringAggDataDTO;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/plugin_aggs")
public class PluginAggregationsController {

    private final ObjectMapper objectMapper;
    private final AggregationsDatabaseService aggregationsDatabaseService;
    private final String aggBucket;
    private final Storage storage;

    @Autowired
    public PluginAggregationsController(ObjectMapper objectMapper, AggregationsDatabaseService aggregationsDatabaseService,
                                        @Value("${AGG_OUTPUT_BUCKET:aggregations-levelops}") String aggBucket, Storage storage) {
        this.objectMapper = objectMapper;
        this.aggregationsDatabaseService = aggregationsDatabaseService;
        this.aggBucket = aggBucket;
        this.storage = storage;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{aggregation_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<JenkinsMonitoringAggDataDTO>> getPluginAgg(@PathVariable("company") String company,
                                                                                    @PathVariable("aggregation_id") UUID aggId ) {
        return SpringUtils.deferResponse(() -> {
            try {
                AggregationRecord agg = aggregationsDatabaseService.get(company, aggId.toString()).orElseThrow();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                storage.get(aggBucket, agg.getGcsPath()).downloadTo(baos);
                JenkinsMonitoringAggData data = objectMapper.readValue(baos.toString(),
                        JenkinsMonitoringAggData.class);

                JenkinsMonitoringAggDataDTO jenkinsMonitoringAggDataDTO = JenkinsMonitoringAggDataDTO.builder()
                        .configChangesByJobs(data.getConfigChangesByJobs())
                        .configChangesByUsers(data.getConfigChangesByUsers())
                        .configChangesByJobsTimeSeries(data.getConfigChangesByJobsTimeSeries())
                        .configChangesByUsersTimeSeries(data.getConfigChangesByUsersTimeSeries())
                        .jobRunsByJobs(data.getJobRunsByJobs())
                        .jobRunsByUsers(data.getJobRunsByUsers())
                        .jobRunsByJobsTimeSeries(data.getJobRunsByJobsTimeSeries())
                        .jobRunsByUsersTimeSeries(data.getJobRunsByUsersTimeSeries())
                        .jobStats(data.getJobStats())
                        .jobStatsTimeSeries(data.getJobStatsTimeSeries())
                        .id(agg.getId())
                        .productIds(agg.getProductIds())
                        .type(agg.getType())
                        .toolType(agg.getToolType())
                        .build();
                return ResponseEntity.ok(jenkinsMonitoringAggDataDTO);
            } catch (NoSuchElementException e) {
                return ResponseEntity.notFound().build();
            }
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AggregationRecord>>> pluginAggsList(@PathVariable("company") String company,
                                                                                               @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String typeString = filter.getFilterValue("type", String.class)
                    .orElse(null);
            AggregationRecord.Type type = EnumUtils.getEnumIgnoreCase(AggregationRecord.Type.class, typeString);
            String toolType = filter.getFilterValue("tool_type", String.class)
                    .orElse(null);
            DbListResponse<AggregationRecord> response = aggregationsDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(),
                    null, (type == null) ? null : List.of(type), (toolType == null) ? null :List.of(toolType));
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),response));
        });
    }
}
