package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class ConfigTableUtils {

    public static final int MAX_ROWS = 16384; // keep in sync with internal api
    public static final int MAX_COLUMNS = 256; // keep in sync with internal api

    /**
     * Converts CSV data into the config table's schema.
     */
    public static ConfigTable fromCsv(Stream<Map<String, String>> csvData) throws IllegalArgumentException {
        return fromCsv(csvData, null, false);
    }

    /**
     * Converts CSV data into the config table's schema.
     *
     * @param csvData                         stream of rows
     * @param columns                         optional; can be used to seed columns in the correct order
     * @param useIntegerIndicesInsteadOfUUIDs if True, integers will be used instead of UUIDs for column & row ids
     * @return data using the config table schema
     * @throws IllegalArgumentException if max # of rows or columns has been exceeded
     */
    public static ConfigTable fromCsv(Stream<Map<String, String>> csvData, @Nullable List<String> columns, boolean useIntegerIndicesInsteadOfUUIDs) throws IllegalArgumentException {
        Map<String, ConfigTable.Column> columnsByName = new HashMap<>();

        Function<String, ConfigTable.Column> createColumn = k -> {
            if (columnsByName.size() >= MAX_COLUMNS) {
                throw new IllegalArgumentException("Exceeded maximum number of columns (" + MAX_COLUMNS + ")");
            }
            var column = ConfigTable.Column.builder()
                    .id(useIntegerIndicesInsteadOfUUIDs ? String.valueOf(columnsByName.size()) : UUID.randomUUID().toString())
                    .index(columnsByName.size())
                    .displayName(k)
                    .key(generateColumnKey(k))
                    .type("string")
                    .build();
            columnsByName.put(k, column);
            return column;
        };

        ListUtils.emptyIfNull(columns).forEach(createColumn::apply);

        MutableInt index = new MutableInt(0);
        Map<String, ConfigTable.Row> rows = csvData
                .map(currentRow -> {
                    Map<String, String> values = new HashMap<>();

                    currentRow.forEach((k, v) -> {
                        ConfigTable.Column column = columnsByName.get(k);
                        if (column == null) {
                            column = createColumn.apply(k);
                        }
                        values.put(column.getId(), v);
                    });

                    return values;
                })
                .map(values -> ConfigTable.Row.builder()
                        .id(useIntegerIndicesInsteadOfUUIDs ? String.valueOf(index.intValue()) : UUID.randomUUID().toString())
                        .index(index.getAndIncrement())
                        .values(values)
                        .build())
                .limit(MAX_ROWS + 1)
                .collect(Collectors.toMap(ConfigTable.Row::getId, row -> row));
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("Exceeded maximum number of rows (" + MAX_ROWS + ")");
        }

        Map<String, ConfigTable.Column> columnsById = columnsByName.values().stream()
                .collect(Collectors.toMap(ConfigTable.Column::getId, v -> v, (a, b) -> b));

        return ConfigTable.builder()
                .schema(ConfigTable.Schema.builder()
                        .columns(columnsById)
                        .build())
                .rows(rows)
                .build();
    }

    // region sanitize
    public static ConfigTable sanitize(ConfigTable configTable) {
        if (configTable == null) {
            return null;
        }
        return configTable.toBuilder()
                .schema(sanitizeSchema(configTable.getSchema()))
                .build();
    }

    protected static ConfigTable.Schema sanitizeSchema(ConfigTable.Schema schema) {
        if (schema == null) {
            return null;
        }

        // sanitize columns
        MutableInt maxIndex = new MutableInt(findMaxColumnIndex(schema.getColumns()));
        var columns = MapUtils.emptyIfNull(schema.getColumns()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> sanitizeColumn(entry.getKey(), entry.getValue(), maxIndex)));

        // check for duplicates
        List<String> keys = columns.values().stream().map(ConfigTable.Column::getKey).collect(Collectors.toList());
        Set<String> uniqueKeys = new HashSet<>(keys);
        if (uniqueKeys.size() != keys.size()) {
            throw new IllegalArgumentException("Column keys must be unique: " + String.join(", ", keys));
        }

        return schema.toBuilder()
                .columns(columns)
                .build();
    }

    public static int findMaxColumnIndex(Map<String, ConfigTable.Column> columns) {
        return MapUtils.emptyIfNull(columns).values().stream()
                .map(ConfigTable.Column::getIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    public static int findMaxRowIndex(Map<String, ConfigTable.Row> rows) {
        return MapUtils.emptyIfNull(rows).values().stream()
                .map(ConfigTable.Row::getIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    protected static ConfigTable.Column sanitizeColumn(String id, ConfigTable.Column column, MutableInt maxIndex) {
        if (!id.equals(column.getId())) {
            throw new IllegalArgumentException("Column id mismatch");
        }
        if (StringUtils.isBlank(column.getDisplayName())) {
            throw new IllegalArgumentException("Column name cannot be empty.");
        }
        Integer index = column.getIndex();
        if (index == null) {
            maxIndex.increment();
            index = maxIndex.intValue();
        }
        return column.toBuilder()
                .id(id)
                .index(index)
                .displayName(column.getDisplayName().trim())
                .key(generateColumnKey(column.getDisplayName()))
                .build();
    }

    public static String generateColumnKey(String columnName) {
        return columnName.trim().replaceAll("\\W", "_").toLowerCase();
    }
    //endregion

    /**
     * Return a map of sanitized (trim, lowercase) column display names to column ids
     */
    @Nonnull
    public static Map<String, String> groupColumnIdsByDisplayName(@Nullable ConfigTable table) {
        if (table == null || table.getSchema() == null || table.getSchema().getColumns() == null) {
            return Collections.emptyMap();
        }
        return table.getSchema().getColumns().values().stream()
                .filter(col -> StringUtils.isNotEmpty(col.getId()))
                .filter(col -> StringUtils.isNotEmpty(col.getDisplayName()))
                .collect(Collectors.toMap(
                        column -> column.getDisplayName().trim().toLowerCase(),
                        ConfigTable.Column::getId,
                        (a, b) -> b));
    }

    @Nonnull
    public static Map<String, ConfigTable.Row> groupRowsByKey(@Nullable ConfigTable table, @Nullable String keyColumnId) {
        if (table == null || table.getRows() == null || keyColumnId == null) {
            return Collections.emptyMap();
        }
        return table.getRows().values().stream()
                .filter(row -> row.getValues() != null)
                .filter(row -> row.getValues().get(keyColumnId) != null)
                .collect(Collectors.toMap(
                        row -> row.getValues().get(keyColumnId),
                        row -> row,
                        (a, b) -> b));
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CsvDiffResult.CsvDiffResultBuilder.class)
    public static class CsvDiffResult {
        Set<String> addedKeys;
        Set<String> removedKeys;
        Set<String> changedKeys;
    }

    public static CsvDiffResult diffByUniqueKey(ConfigTable previousData, ConfigTable currentData, String keyColumnDisplayName, Set<String> displayNamesOfColumnsToDiff) {
        Validate.notBlank(keyColumnDisplayName, "keyColumnDisplayName cannot be null or empty.");

        // sanitize (trim, lowercase)
        keyColumnDisplayName = keyColumnDisplayName.trim().toLowerCase();
        displayNamesOfColumnsToDiff = SetUtils.emptyIfNull(displayNamesOfColumnsToDiff).stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());;

        // get a map of all the columns ids grouped by their display name
        Map<String, String> prevColumnIdsByName = groupColumnIdsByDisplayName(previousData);
        Map<String, String> currColumnIdsByName = groupColumnIdsByDisplayName(currentData);

        log.debug("prevColumnIdsByName={}", prevColumnIdsByName);
        log.debug("currColumnIdsByName={}", currColumnIdsByName);

        // find out the id of the column containing the unique key (it may be missing!)
        String prevKeyColumnId = prevColumnIdsByName.get(keyColumnDisplayName);
        String currKeyColumnId = currColumnIdsByName.get(keyColumnDisplayName);
        log.debug("keyColumn='{}', prevKeyColumnId = {}, currKeyColumnId={}", keyColumnDisplayName, prevKeyColumnId, currKeyColumnId);

        // get a map of rows grouped by the unique key (it may be empty if the key column was not found)
        Map<String, ConfigTable.Row> prevRowsByKey = groupRowsByKey(previousData, prevKeyColumnId);
        Map<String, ConfigTable.Row> currRowsByKey = groupRowsByKey(currentData, currKeyColumnId);

        // get a map of columns ids to diff on grouped by the column name
        Map<String, String> prevColumnIdsToDiffByName = MapUtils.extract(prevColumnIdsByName, displayNamesOfColumnsToDiff);
        Map<String, String> currColumnIdsToDiffByName = MapUtils.extract(currColumnIdsByName, displayNamesOfColumnsToDiff);

        // -- DIFF

        // additions:
        Set<String> addedKeys = SetUtils.difference(currRowsByKey.keySet(), prevRowsByKey.keySet());

        // deletions:
        Set<String> removedKeys = SetUtils.difference(prevRowsByKey.keySet(), currRowsByKey.keySet());

        // changes:
        Set<String> changedKeys = new HashSet<>();
        for (String commonKey : SetUtils.intersection(currRowsByKey.keySet(), prevRowsByKey.keySet())) {
            ConfigTable.Row prevRow = prevRowsByKey.get(commonKey);
            ConfigTable.Row currRow = currRowsByKey.get(commonKey);
            for (String col : displayNamesOfColumnsToDiff) {
                String prevColId = prevColumnIdsToDiffByName.get(col);
                String currColId = currColumnIdsToDiffByName.get(col);
                // -- check if the columns themselves have been added/removed
                if (prevColId == null && currColId == null) {
                    // no change
                    continue;
                }
                if (prevColId != null && currColId == null) {
                    changedKeys.add(commonKey);
                    break;
                }
                if (prevColId == null && currColId != null) {
                    changedKeys.add(commonKey);
                    break;
                }
                // -- compare values
                String prevValue = prevRow.getValues().get(prevColId);
                String currValue = currRow.getValues().get(currColId);
                if (!StringUtils.equals(prevValue, currValue)) {
                    changedKeys.add(commonKey);
                    break;
                }
            }
        }

        return CsvDiffResult.builder()
                .addedKeys(addedKeys)
                .removedKeys(removedKeys)
                .changedKeys(changedKeys)
                .build();
    }

    public static Map<String, ConfigTable.Row> getRowsByUniqueKeys(ConfigTable table, String keyColumnDisplayName, Set<String> uniqueKeys) {
        if (table == null || table.getRows() == null || StringUtils.isBlank(keyColumnDisplayName) || CollectionUtils.isEmpty(uniqueKeys)) {
            return Collections.emptyMap();
        }
        Map<String, String> columnIdsByName = groupColumnIdsByDisplayName(table);
        String keyColumnId = columnIdsByName.get(keyColumnDisplayName.trim().toLowerCase());
        Map<String, ConfigTable.Row> rowsByKey = groupRowsByKey(table, keyColumnId);
        return MapUtils.filter(rowsByKey, kv -> uniqueKeys.contains(kv.getKey()));
    }

    public static Map<String, ConfigTable.Row> getRowsByUniqueKeysUsingColumnId(ConfigTable table, String keyColumnId, Set<String> uniqueKeys) {
        if (table == null || table.getRows() == null || StringUtils.isBlank(keyColumnId) || CollectionUtils.isEmpty(uniqueKeys)) {
            return Collections.emptyMap();
        }
        Map<String, ConfigTable.Row> rowsByKey = groupRowsByKey(table, keyColumnId);
        return MapUtils.filter(rowsByKey, kv -> uniqueKeys.contains(kv.getKey()));
    }

}
