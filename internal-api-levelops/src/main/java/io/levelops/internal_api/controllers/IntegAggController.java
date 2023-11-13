package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/product_aggs")
public class IntegAggController {
    private final Storage storage;
    private final String aggBucket;
    private final ObjectMapper objectMapper;
    private final IntegrationAggService integrationAggService;

    @Autowired
    public IntegAggController(IntegrationAggService integrationAggService, Storage storage, ObjectMapper objectMapper,
                              @Value("${AGG_OUTPUT_BUCKET:aggregations-levelops}") String aggBucket) {
        this.storage = storage;
        this.aggBucket = aggBucket;
        this.integrationAggService = integrationAggService;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{aggregationid}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> getIntegrationAgg(@PathVariable("company") String company,
                                                                    @PathVariable("aggregationid") String aggId) {
        return SpringUtils.deferResponse(() -> {
            try {
                IntegrationAgg agg = integrationAggService.get(company, aggId).orElseThrow();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                storage.get(aggBucket, agg.getGcsPath()).downloadTo(baos);
                Map<String, Object> data = objectMapper.readValue(baos.toString(),
                        new TypeReference<HashMap<String, Object>>() {
                        });
                //remove unnecessary metadata from the object
                data.remove("_levelops_agg_version");
                data.remove("_levelops_ingestion_data");
                data.put("id", agg.getId());
                data.put("integration_ids", agg.getIntegrationIds());
                data.put("type", agg.getType());
                return ResponseEntity.ok(objectMapper.writeValueAsString(data));
            } catch (NoSuchElementException e) {
                return ResponseEntity.notFound().build();
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<IntegrationAgg>>> integrationAggsList(@PathVariable("company") String company,
                                                                                                 @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String productId = filter.getFilterValue("product_id", String.class)
                    .orElse(null);
            List<String> integrationIds = (List<String>) filter.getFilter().get("integration_ids");
            List<String> aggType = (List<String>) filter.getFilter().get("integration_types");
            List<IntegrationAgg.AnalyticType> types = new ArrayList<>();
            if (aggType != null) {
                aggType.forEach(data -> types.add(IntegrationAgg.AnalyticType.fromString(data)));
            }

            DbListResponse<IntegrationAgg> response = integrationAggService.listByFilter(company,
                    types, integrationIds, true, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), response));
        });
    }
}
