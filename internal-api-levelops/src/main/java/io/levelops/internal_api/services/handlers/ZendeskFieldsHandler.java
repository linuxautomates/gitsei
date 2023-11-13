package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.integrations.zendesk.models.Field;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Service
@Log4j2
public class ZendeskFieldsHandler extends IntegrationDataHandler {

    private final ZendeskFieldService zendeskFieldService;

    @Autowired
    public ZendeskFieldsHandler(ZendeskFieldService zendeskFieldService, Storage storage,
                                ObjectMapper objectMapper) {
        super(storage, objectMapper);
        this.zendeskFieldService = zendeskFieldService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.ZENDESK;
    }

    @Override
    public String getDataType() {
        return "fields";
    }

    @Override
    public Boolean handleStorageResult(String company, String integrationId, StorageResult storageResult)
            throws IOException, SQLException {
        if (!getDataType().equalsIgnoreCase(storageResult.getStorageMetadata().getDataType()))
            return false;
        List<String> dataList = getDataToPush(storageResult.getRecords());
        List<DbZendeskField> zendeskFields = new ArrayList<>();
        List<DbZendeskField> existingFields = zendeskFieldService.listByFilter(company,
                List.of(integrationId),
                null,
                null,
                null,
                null,
                0,
                1000000).getRecords();
        Map<String, DbZendeskField> existingMap = new HashMap<>();
        existingFields.forEach(field -> existingMap.put(field.getId(), field));
        for (String data : dataList) {
            StorageContent<ListResponse<Field>> fields = objectMapper.readValue(data,
                    StorageContent.getListStorageContentJavaType(objectMapper, Field.class));
            fields.getData().getRecords().stream()
                    .map(record -> DbZendeskField.fromZendeskField(record, integrationId))
                    .filter(Objects::nonNull)
                    .filter(record -> {
                        //if nothing is changed with the record, no need to attempt to insert.
                        DbZendeskField existingField = existingMap.get(record.getId());
                        if (existingField == null) return true;
                        if (!StringUtils.equals(existingField.getTitle(), record.getTitle()))
                            return true;
                        if (!StringUtils.equals(existingField.getFieldType(), record.getFieldType()))
                            return true;
                        return !StringUtils.equals(existingField.getDescription(), record.getDescription());
                    })
                    .forEach(zendeskFields::add);
        }
        if (zendeskFields.size() > 0) {
            zendeskFieldService.batchUpsert(company, zendeskFields);
        }
        log.info("Finished Zendesk Fields for company: {}, integrationid: {}", company, integrationId);
        return true;
    }
}
