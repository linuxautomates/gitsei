package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.node.TextNode;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.services.ConfigTableDatabaseService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.internal_api.services.ConfigTableService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/config-tables")
public class ConfigTableController {

    private static final int MAX_ROWS = 16384;
    private static final int MAX_COLUMNS = 256;
    private final ConfigTableService configTableService;

    public ConfigTableController(ConfigTableService configTableService) {
        this.configTableService = configTableService;
    }

    private static void validate(ConfigTable configTable) throws BadRequestException {
        if (configTable == null) {
            throw new BadRequestException("Config table data required");
        }
        int nbOfRows = MapUtils.size(configTable.getRows());
        if (nbOfRows > MAX_ROWS) {
            throw new BadRequestException("Exceeded maximum number of rows (" + nbOfRows + " > " + MAX_ROWS + ")");
        }
        int nbOfColumns = MapUtils.emptyIfNull(configTable.getRows()).values().stream()
                .map(ConfigTable.Row::getValues)
                .map(MapUtils::size)
                .max(Integer::compareTo)
                .orElse(0);
        if (nbOfColumns > MAX_COLUMNS) {
            throw new BadRequestException("Exceeded maximum number of columns (" + nbOfColumns + " > " + MAX_COLUMNS + ")");
        }
    }


    @GetMapping("/{id}")
    public DeferredResult<ResponseEntity<ConfigTable>> get(@PathVariable("company") final String company,
                                                           @PathVariable("id") String id,
                                                           @RequestParam(value = "expand", required = false, defaultValue = "") List<String> expand) {
        return SpringUtils.deferResponse(() -> {
            ConfigTable table = configTableService.get(company, id,
                    ConfigTableDatabaseService.getExpand(expand.contains("schema"), expand.contains("rows"), expand.contains("history")))
                    .orElseThrow(() -> new NotFoundException("Could not find config table with id=" + id));
            return ResponseEntity.ok().body(table);
        });
    }

    @PostMapping("/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<ConfigTable>>> list(@PathVariable("company") final String company,
                                                                               @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            List<String> fields = ListUtils.emptyIfNull(request.getFields());
            DbListResponse<ConfigTable> tables = configTableService.filter(request.getPage(), request.getPageSize(), company,
                    request.<String>getFilterValueAsList("ids").orElse(null),
                    request.<String>getFilterValue("name", String.class).orElse(null),
                    request.getFilterValueAsMap("partial").map(m -> (String) m.get("name")).orElse(null),
                    ConfigTableDatabaseService.getExpand(fields.contains("schema"), fields.contains("rows"), fields.contains("history")));
            return ResponseEntity.ok().body(PaginatedResponse.of(request.getPage(), request.getPageSize(), tables));
        });
    }

    /**
     * Creates a new config table.
     * Required fields: name
     * Optional fields: schema, rows, creator
     */
    @PostMapping
    public DeferredResult<ResponseEntity<Map<String, String>>> create(@PathVariable("company") final String company,
                                                                      @RequestBody ConfigTable configTable) {

        return SpringUtils.deferResponse(() -> {
            validate(configTable);
            String id = configTableService.insert(company, configTable)
                    .orElseThrow(() -> new BadRequestException("Could not create config table. Make sure name is unique."));
            return ResponseEntity.ok().body(Map.of("id", id, "version", ConfigTableDatabaseService.DEFAULT_VERSION));
        });
    }

    /**
     * Creates a new config table.
     * Required fields: id
     * Optional fields: schema, rows, creator, updated_by (recommended)
     */
    @PutMapping("/{id}")
    public DeferredResult<ResponseEntity<Map<String, ?>>> update(@PathVariable("company") final String company,
                                                                 @PathVariable("id") String id,
                                                                 @RequestBody ConfigTable configTable) {
        return SpringUtils.deferResponse(() -> {
            validate(configTable);
            String version = configTableService.updateAndReturnVersion(company, id, configTable);
            if (version == null) {
                throw new BadRequestException("Could not update config table. Make sure name is unique.");
            }
            return ResponseEntity.ok().body(Map.of("id", id, "version", version));
        });
    }

    /**
     * THIS DOES NOT BUMP THE TABLE'S VERSION!
     */
    @PutMapping("/{id}/rows/{rowId}")
    public DeferredResult<ResponseEntity<Map<String, ?>>> updateRow(@PathVariable("company") final String company,
                                                                    @PathVariable("id") String id,
                                                                    @PathVariable("rowId") String rowId,
                                                                    @RequestBody ConfigTable.Row row) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(rowId) || !rowId.equals(row.getId())) {
                throw new BadRequestException("Row Id from request path doesn't match request body");
            }
            Boolean success = configTableService.updateRowWithoutBumpingVersion(company, id, row);
            return ResponseEntity.ok().body(Map.of(
                    "id", id,
                    "row_id", rowId,
                    "success", success));
        });
    }

    /**
     * THIS DOES NOT BUMP THE TABLE'S VERSION!
     */
    @PostMapping("/{id}/rows")
    public DeferredResult<ResponseEntity<Map<String, ?>>> insertRow(@PathVariable("company") final String company,
                                                                    @PathVariable("id") String id,
                                                                    @RequestBody ConfigTable.Row row) {
        return SpringUtils.deferResponse(() -> {
            ConfigTable.Row newRowInserted = configTableService.insertRowWithoutBumpingVersion(company, id, row);
            boolean success = newRowInserted != null;
                Map<String, Object> response = new HashMap<>(Map.of(
                        "table_id", id,
                        "success", success));
                if (success) {
                    response.put("row_id", newRowInserted.getId());
                    response.put("index", newRowInserted.getIndex());
                }
                return ResponseEntity.ok().body(response);
        });
    }

    /**
     * THIS DOES NOT BUMP THE TABLE'S VERSION!
     */
    @PutMapping("/{id}/rows/{rowId}/columns/{columnNameOrId}")
    public DeferredResult<ResponseEntity<Map<String, ?>>> updateRow(@PathVariable("company") final String company,
                                                                    @PathVariable("id") String id,
                                                                    @PathVariable("rowId") String rowId,
                                                                    @PathVariable("columnNameOrId") String columnNameOrId,
                                                                    @RequestBody TextNode value) {
        return SpringUtils.deferResponse(() -> {
            Boolean success = configTableService.updateColumnWithoutBumpingVersion(company, id, rowId, columnNameOrId, value.textValue());
            return ResponseEntity.ok().body(Map.of(
                    "id", id,
                    "row_id", rowId,
                    "column", columnNameOrId,
                    "success", success));
        });
    }

    @DeleteMapping("/{id}")
    public DeferredResult<ResponseEntity<DeleteResponse>> delete(@PathVariable("company") final String company,
                                                                 @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
            try {
                configTableService.delete(company, id);
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder()
                        .id(id)
                        .success(false)
                        .error(e.getMessage())
                        .build());
            }
            return ResponseEntity.ok(DeleteResponse.builder()
                    .id(id)
                    .success(true)
                    .build());
        });
    }

    @DeleteMapping
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkdelete(@PathVariable("company") final String company,
                                                                         @RequestBody List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            List<DeleteResponse> records = configTableService.bulkDelete(company, ids);
            return ResponseEntity.ok(BulkDeleteResponse.of(records));
        });
    }

    @GetMapping("/{id}/revisions/{version}")
    public DeferredResult<ResponseEntity<ConfigTable>> getRevision(@PathVariable("company") final String company,
                                                                   @PathVariable("id") String id,
                                                                   @PathVariable("version") String version) {
        return SpringUtils.deferResponse(() -> {
            ConfigTable table = configTableService.getRevision(company, id, version)
                    .orElseThrow(() -> new NotFoundException("Could not find revision of table " + id + " with version " + version));
            return ResponseEntity.ok().body(table);
        });
    }

}
