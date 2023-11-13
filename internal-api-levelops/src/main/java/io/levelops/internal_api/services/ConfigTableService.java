package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.converters.ConfigTableUtils;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.services.ConfigTableDatabaseService;
import io.levelops.commons.databases.services.ConfigTableDatabaseService.Field;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.web.exceptions.ConflictException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class ConfigTableService {

    private static final String CONFIG_TABLE_LOCK = "lock_config_table";
    private static final int CONFIG_TABLE_LOCK_EXPIRATION_SECS = 30;
    private static final int CONFIG_TABLE_LOCK_WAIT_SECS = 60;
    LockRegistry lockRegistry;
    private final String bucketName;
    private final int maxRevisions;
    private final ConfigTableDatabaseService configTableDatabaseService;
    private final ObjectMapper objectMapper;
    private final Storage storage;

    @Autowired
    public ConfigTableService(ConfigTableDatabaseService configTableDatabaseService,
                              RedisConnectionFactory redisConnectionFactory,
                              ObjectMapper objectMapper,
                              Storage storage,
                              @Value("${UPLOADS_BUCKET_NAME}") String bucketName,
                              @Value("${CONFIG_TABLE_MAX_REVISIONS:10}") int maxRevisions) {
        this.configTableDatabaseService = configTableDatabaseService;
        this.objectMapper = objectMapper;
        this.storage = storage;
        lockRegistry = new RedisLockRegistry(redisConnectionFactory, CONFIG_TABLE_LOCK, TimeUnit.SECONDS.toMillis(CONFIG_TABLE_LOCK_EXPIRATION_SECS));
        this.bucketName = bucketName;
        this.maxRevisions = maxRevisions;
    }

    protected static ConfigTable addCurrentVersionToHistoryAndSort(ConfigTable configTable) {
        if (configTable == null) {
            return null;
        }
        if (configTable.getHistory() == null || StringUtils.isEmpty(configTable.getVersion())) {
            return configTable;
        }

        Map<String, ConfigTable.Revision> currentRevision = Map.of(configTable.getVersion(), ConfigTable.Revision.builder()
                .version(configTable.getVersion())
                .userId(StringUtils.defaultIfBlank(configTable.getUpdatedBy(), configTable.getCreatedBy()))
                .createdAt((Instant) ObjectUtils.defaultIfNull(configTable.getUpdatedAt(), configTable.getCreatedAt()))
                .build());

        LinkedHashMap<String, ConfigTable.Revision> sorted = new LinkedHashMap<>();
        Stream.concat(configTable.getHistory().entrySet().stream(), currentRevision.entrySet().stream())
                .sorted(Map.Entry.<String, ConfigTable.Revision>comparingByKey(Comparator.comparingInt(Integer::parseInt)).reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));

        return configTable.toBuilder()
                .history(sorted)
                .build();
    }

    public Optional<ConfigTable> get(String company, String tableId, EnumSet<Field> expand) throws SQLException {
        return configTableDatabaseService.get(company, tableId, expand)
                .map(ConfigTableService::addCurrentVersionToHistoryAndSort);
    }

    /**
     * Creates a new config table.
     * Required fields: name
     * Optional fields: schema, rows, created_by
     *
     * @return
     */
    public Optional<String> insert(String company, ConfigTable configTable) throws SQLException {
        return configTableDatabaseService.insertAndReturnId(company, ConfigTableUtils.sanitize(configTable));
    }

    public DbListResponse<ConfigTable> filter(Integer page, Integer pageSize, String company, @Nullable List<String> ids, @Nullable String exactName, @Nullable String partialName, EnumSet<Field> expand) {
        DbListResponse<ConfigTable> response = configTableDatabaseService.filter(page, pageSize, company, ids, exactName, partialName, expand);
        if (response == null) {
            return DbListResponse.of(List.of(), 0);
        }
        List<ConfigTable> configTables = ListUtils.emptyIfNull(response.getRecords()).stream()
                .map(ConfigTableService::addCurrentVersionToHistoryAndSort)
                .collect(Collectors.toList());
        return DbListResponse.of(configTables, response.getTotalCount());
    }

    @Nullable
    private <T> T doInLock(String company, String tableId, Callable<T> callable) throws Exception {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(tableId, "tableId cannot be null or empty.");
        Lock lock = lockRegistry.obtain(company + tableId);
        if (!lock.tryLock(CONFIG_TABLE_LOCK_WAIT_SECS, TimeUnit.SECONDS)) {
            throw new ConflictException("Failed to update config table: lock already taken - company=" + company + " id=" + tableId);
        }
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    public String updateAndReturnVersion(String company, String tableId, ConfigTable newTable) throws Exception {
        return doInLock(company, tableId, () -> {
            try {
                ConfigTable oldTable = configTableDatabaseService.get(company, tableId, EnumSet.allOf(Field.class))
                        .orElseThrow(() -> new NotFoundException("Could not find config table with id=" + tableId));

                // -- store current data as revision first
                storeRevisionData(company, tableId, oldTable.getVersion(), oldTable);

                configTableDatabaseService.insertRevision(company, tableId, ConfigTable.Revision.builder()
                        .version(oldTable.getVersion())
                        .createdAt((Instant) ObjectUtils.defaultIfNull(oldTable.getUpdatedAt(), oldTable.getCreatedAt()))
                        .userId(StringUtils.firstNonBlank(oldTable.getUpdatedBy(), oldTable.getCreatedBy()))
                        .build());

                ConfigTable update = ConfigTable.builder()
                        .id(tableId)
                        .name(newTable.getName())
                        .schema(newTable.getSchema())
                        .rows(newTable.getRows())
                        .version(ConfigTableDatabaseService.INCREMENT_VERSION)
                        .updatedBy(newTable.getUpdatedBy())
                        .build();

                // -- then insert table (in case of error & retry, the revision will just be overwritten)
                var table = configTableDatabaseService.updateAndReturnVersion(company, ConfigTableUtils.sanitize(update))
                        .orElse(oldTable.getVersion());

                // -- delete older revisions
                // the new table will have: #newTable.revisions = #oldTable.revisions + 1 (current version) + 1 (new version, not stored in db)
                // so from the table before update, we need to keep:
                // maxRevisions - 1 (current version after update) - 1 (version before update)
                // so that total # of revisions + current version = maxRevisions
                List<String> versionsToRemove = findVersionsToRemove(oldTable, maxRevisions - 2);
                deleteRevisions(company, tableId, versionsToRemove);

                return table;
            } catch (DuplicateKeyException e) {
                log.warn("Could not update config table", e);
                return null;
            }
        });
    }

    public Boolean updateRowWithoutBumpingVersion(String company, String tableId, ConfigTable.Row row) throws Exception {
        Validate.notNull(row, "row cannot be null.");
        Validate.notBlank(row.getId(), "row.getId() cannot be null or empty.");

        return doInLock(company, tableId, () -> {
            ConfigTable oldTable = configTableDatabaseService.get(company, tableId, EnumSet.of(Field.ROWS, Field.SCHEMA))
                    .orElseThrow(() -> new NotFoundException("Could not find config table with id=" + tableId));

            for (Map.Entry<String, String> entry : row.getValues().entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();
                row.getValues().remove(column, value);
                String columnId = findColumnId(oldTable, column)
                        .orElseThrow(() -> new IllegalArgumentException("Could not find column with id or name '" + column + "' in table='" + tableId + "' version=" + oldTable.getVersion() + "  for customer=" + company));
                row.getValues().put(columnId, value);
            }

            ConfigTable update = ConfigTable.builder()
                    .rows(MapUtils.append(oldTable.getRows(), row.getId(), row))
                    .build();

            return configTableDatabaseService.update(company, update);
        });
    }

    public ConfigTable.Row insertRowWithoutBumpingVersion(String company, String tableId, ConfigTable.Row row) throws Exception {
        Validate.notNull(row, "row cannot be null.");

        return doInLock(company, tableId, () -> {

            ConfigTable oldTable = configTableDatabaseService.get(company, tableId, EnumSet.of(Field.ROWS, Field.SCHEMA))
                    .orElseThrow(() -> new NotFoundException("Could not find config table with id=" + tableId));

            Map<String, String> sanitizedValues = MapUtils.emptyIfNull(row.getValues()).entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> findColumnId(oldTable, entry.getKey()).orElseThrow(() -> new IllegalArgumentException("Could not find column with id or name '" + entry.getKey() + "' in table='" + tableId + "' version=" + oldTable.getVersion() + "  for customer=" + company)),
                            Map.Entry::getValue,
                            (a, b) -> b));
            String newRowId = String.valueOf(UUID.randomUUID());
            List<Integer> rowIndices = oldTable.getRows().values()
                    .stream()
                    .map(ConfigTable.Row::getIndex)
                    .collect(Collectors.toList());
            Integer newRowIndex = Collections.max(rowIndices) + 1;
            ConfigTable.Row newRow = row.toBuilder()
                    .id(newRowId)
                    .index(newRowIndex)
                    .values(sanitizedValues)
                    .build();
            ConfigTable insert = ConfigTable.builder()
                    .id(tableId)
                    .rows(MapUtils.append(oldTable.getRows(), newRowId, newRow))
                    .build();

            Boolean update = configTableDatabaseService.update(company, insert);
            if (update) {
                return newRow;
            } else {
                return null;
            }
        });
    }

    public Boolean updateColumnWithoutBumpingVersion(String company, String tableId, String rowId, String columnNameOrId, String value) throws Exception {
        Validate.notBlank(rowId, "rowId cannot be null or empty.");
        Validate.notBlank(columnNameOrId, "columnNameOrId cannot be null or empty.");
        return doInLock(company, tableId, () -> {
            ConfigTable oldTable = configTableDatabaseService.get(company, tableId, EnumSet.of(Field.ROWS, Field.SCHEMA))
                    .orElseThrow(() -> new NotFoundException("Could not find config table with id=" + tableId));
            Map<String, ConfigTable.Row> rows = MapUtils.emptyIfNull(oldTable.getRows());

            ConfigTable.Row row = rows.get(rowId);
            if (row == null) {
                throw new NotFoundException("Row id=" + rowId + "not found in table id=" + tableId);
            }

            String columnId = findColumnId(oldTable, columnNameOrId)
                    .orElseThrow(() -> new IllegalArgumentException("Could not find column with id or name '" + columnNameOrId + "' in table='" + tableId + "' version=" + oldTable.getVersion() + "  for customer=" + company));

            ConfigTable.Row updatedRow = row.toBuilder()
                    .values(io.levelops.commons.utils.MapUtils.append(row.getValues(), columnId, value))
                    .build();

            rows = io.levelops.commons.utils.MapUtils.append(rows, rowId, updatedRow);

            ConfigTable update = ConfigTable.builder()
                    .id(tableId)
                    .rows(rows)
                    .build();

            return configTableDatabaseService.update(company, update);
        });
    }

    private Optional<String> findColumnId(@Nullable ConfigTable table, String columnNameOrId) {
        Validate.notBlank(columnNameOrId, "columnNameOrId cannot be null or empty.");
        boolean isValidId = validateColumnId(table, columnNameOrId);
        if (isValidId) {
            return Optional.of(columnNameOrId);
        }
        return findColumnIdByName(table, columnNameOrId);
    }

    private boolean validateColumnId(ConfigTable table, String columnId) {
        Validate.notBlank(columnId, "columnId cannot be null or empty.");
        if (table == null || table.getSchema() == null) {
            return false;
        }
        return MapUtils.emptyIfNull(table.getSchema().getColumns()).containsKey(columnId);
    }

    private Optional<String> findColumnIdByName(ConfigTable table, String columnName) {
        Validate.notBlank(columnName, "columnName cannot be null or empty.");
        if (table == null || table.getSchema() == null) {
            throw new IllegalArgumentException("Could not find column in missing or empty table");
        }
        return MapUtils.emptyIfNull(table.getSchema().getColumns()).values().stream()
                .filter(Objects::nonNull)
                .filter(col -> columnName.trim().equalsIgnoreCase(StringUtils.trim(col.getDisplayName())))
                .map(ConfigTable.Column::getId)
                .filter(StringUtils::isNotBlank)
                .findFirst();
    }


    @Nonnull
    protected static List<String> findVersionsToRemove(ConfigTable dbTable, int maxRevisions) {
        if (MapUtils.size(dbTable.getHistory()) <= maxRevisions) {
            return Collections.emptyList();
        }
        return dbTable.getHistory().values().stream()
                .map(ConfigTable.Revision::getVersion)
                .map(Integer::parseInt)
                .sorted(Comparator.comparingInt(x -> (Integer) x).reversed())
                .skip(maxRevisions) // current version is not stored in db so we want to keep n - 1 versions
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    public boolean delete(String company, String tableId) throws InterruptedException, ConflictException, SQLException {
        Lock lock = lockRegistry.obtain(company + tableId);
        if (!lock.tryLock(CONFIG_TABLE_LOCK_WAIT_SECS, TimeUnit.SECONDS)) {
            throw new ConflictException("Failed to delete config table: lock already taken - company=" + company + " id=" + tableId);
        }
        try {
            if (!configTableDatabaseService.delete(company, tableId)) {
                log.debug("delete : Company {}, Config Table with Id {} is not found and hence not deleted", company, tableId);
                return false;
            }
            deleteAllTableData(company, tableId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public List<DeleteResponse> bulkDelete(String company, List<String> ids) {
        return ids.stream()
                .map(id -> {
                    try {
                        delete(company, id);
                        return DeleteResponse.builder()
                                .id(id)
                                .success(true)
                                .build();
                    } catch (Exception e) {
                        return DeleteResponse.builder()
                                .id(id)
                                .success(false)
                                .error(e.getMessage())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    public Optional<ConfigTable> getRevision(String company, String tableId, String version) {
        try {
            Optional<ConfigTable> dbTable = get(company, tableId, EnumSet.allOf(Field.class));
            if (dbTable.isEmpty()) {
                return Optional.empty();
            }
            if (version.equals(dbTable.get().getVersion())) {
                return dbTable;
            }
            ConfigTable table = fetchRevisionData(company, tableId, version);
            return Optional.of(table.toBuilder()
                    .history(dbTable.get().getHistory())
                    .build());
        } catch (IOException | SQLException e) {
            log.warn("Failed to get revision of table_id={}, version={}", tableId, version);
            return Optional.empty();
        }
    }

    private void deleteRevisions(String company, String tableId, List<String> versionsToRemove) {
        versionsToRemove.forEach(v -> {
            try {
                deleteRevisionData(company, tableId, v);
            } catch (IOException e) {
                log.error("Failed to remove revision of table id={}, version={}", tableId, v, e);
            }
        });
        try {
            configTableDatabaseService.deleteRevisions(company, tableId, versionsToRemove);
        } catch (SQLException e) {
            log.warn("Failed to remove revisions from db for table id={}, versions={}", tableId, versionsToRemove, e);
        }
    }

    private void storeRevisionData(String company, String tableId, String version, ConfigTable data) throws JsonProcessingException {
        String path = generatePath(company, tableId, version);
        byte[] bytes = objectMapper.writeValueAsBytes(data);
        uploadDataToGcs(bucketName, path, "application/json", bytes);
    }

    private ConfigTable fetchRevisionData(String company, String tableId, String version) throws IOException {
        String path = generatePath(company, tableId, version);
        byte[] bytes = downloadDataFromGcs(bucketName, path);
        return objectMapper.readValue(bytes, ConfigTable.class);
    }

    private void deleteRevisionData(String company, String tableId, String version) throws IOException {
        String path = generatePath(company, tableId, version);
        deleteDataFromGcs(path);
    }

    private void deleteAllTableData(String company, String tableId) {
        Iterable<Blob> blobs = listBlobs(generatePathPrefix(company, tableId), true);
        blobs.forEach(b -> deleteDataFromGcs(b.getName()));
    }

    private static String generatePath(String company, String tableId, String version) {
        // ADDED '/' AT THE BEGINNING FOR CONSISTENCY BUT IT SHOULD BE REMOVED!!!
        return generatePathPrefix(company, tableId) + String.format("version-%s.json", version);
    }

    private static String generatePathPrefix(String company, String tableId) {
        // ADDED '/' AT THE BEGINNING FOR CONSISTENCY BUT IT SHOULD BE REMOVED!!!
        return String.format("/%s/config_tables/table-%s/", company, tableId);
    }

    //region GCS utils
    private Iterable<Blob> listBlobs(String pathPrefix, boolean recursive) {
        ArrayList<Storage.BlobListOption> listOptions = new ArrayList<>();
        listOptions.add(Storage.BlobListOption.prefix(pathPrefix));
        if (!recursive) {
            listOptions.add(Storage.BlobListOption.currentDirectory());
        }
        return storage.list(bucketName, listOptions.toArray(Storage.BlobListOption[]::new)).iterateAll();
    }

    private byte[] downloadDataFromGcs(String bucketName, String path) {
        log.info("Downloading content from {}:{}", bucketName, path);
        Blob blob = storage.get(BlobId.of(bucketName, path));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toByteArray();
    }

    private Blob uploadDataToGcs(String bucketName, String path, String contentType, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        log.info("Uploading '{}' content to {}:{}", contentType, bucketName, path);
        return storage.create(blobInfo, content);
    }

    private void deleteDataFromGcs(String path) {
        storage.delete(bucketName, path);
    }

    // endregion
}
