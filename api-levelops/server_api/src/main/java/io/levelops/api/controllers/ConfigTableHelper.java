package io.levelops.api.controllers;

import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.ConfigTableDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@Log4j2
@Service
@SuppressWarnings("unused")
public class ConfigTableHelper {
    private final ConfigTableDatabaseService configTableDatabaseService;
    private final IntegrationService integService;
    private final IntegrationTrackingService integrationTrackingService;

    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    @Autowired
    public ConfigTableHelper(ConfigTableDatabaseService configTableDatabaseService, IntegrationService integService, IntegrationTrackingService integrationTrackingService) {
        this.configTableDatabaseService = configTableDatabaseService;
        this.integService = integService;
        this.integrationTrackingService = integrationTrackingService;
    }

    public Long getIngestedAt(String company, IntegrationType type, DefaultListRequest filter)
            throws SQLException {
        Integration integ = integService.listByFilter(company, null, List.of(type.toString()), null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(x -> NumberUtils.toInt(x))
                        .collect(Collectors.toList()),
                List.of(), 0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integ != null)
            ingestedAt = integrationTrackingService.get(company, integ.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    public boolean isConfigBasedAggregation(DefaultListRequest filter) {
        return StringUtils.isNotEmpty((String) filter.getFilter().get("config_table_id"));
    }

    public boolean isValidConfigTableKey(String configTableId) {
        return UUID_REGEX_PATTERN.matcher(configTableId).matches();
    }

    public ConfigTable validateAndReturnTableForList(String company, DefaultListRequest filter) throws SQLException {
        String configTableId = filter.getFilter().get("config_table_id").toString();
        if(!isValidConfigTableKey(configTableId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid config table key parameter. Required UUID. Provided: " + configTableId);

        ConfigTable configTable = configTableDatabaseService.get(company, configTableId).orElse(null);
        checkRequired(configTable, "Config table not found for given config_table_id");

        Object configTableRowId = filter.getFilter().get("config_table_row_id");
        checkRequired(configTableRowId, "Config table row id (config_table_row_id) parameter not found");

        ConfigTable.Row row = configTable.getRows().get(configTableRowId.toString());
        checkRequired(row, "row not found for given row_id in the config table");

        return configTable;
    }

    public ConfigTable validateAndReturnTableForReport(String company, DefaultListRequest filter) throws SQLException {
        String configTableId = filter.getFilter().get("config_table_id").toString();
        if(!isValidConfigTableKey(configTableId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid config table key parameter. Required UUID. Provided: " + configTableId);

        ConfigTable configTable = configTableDatabaseService.get(company, configTableId).orElse(null);
        checkRequired(configTable, "Config table not found for given config_table_id");

        checkRequired(filter.getAcross(), "No across value found for operation");

        ConfigTable.Column acrossColumn = getColumn(configTable, filter.getAcross());
        checkRequired(acrossColumn, "No column found for given across field");

        if (acrossColumn.getMultiValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Across column should be single value");
        }

        return configTable;
    }

    public ConfigTable.Column getColumn(ConfigTable configTable, String key) {
        return configTable.getSchema().getColumns().values().stream()
                .filter(column -> (column.getKey().equalsIgnoreCase(key)))
                .findAny().orElse(null);
    }

    private void checkRequired(Object required, String message) {
        if (required instanceof String && StringUtils.isEmpty((String) required)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        } else if (required == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
