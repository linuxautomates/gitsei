package io.levelops.commons.databases.services.temporary;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.levelops.integrations.pagerduty.models.PagerDutyEntity;
import io.levelops.integrations.pagerduty.models.PagerDutyTransitionalEntity;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PagerDutyQueryTable extends StreamsDefaultQueryTable<PagerDutyEntity> {

    public PagerDutyQueryTable(DataSource dataSource, String tenant, String tableName, ObjectMapper objectMapper,
            final int batchSize) throws SQLException {
        super(dataSource, tenant, tableName, objectMapper, batchSize);
        createTempTable();
    }

    @Override
    protected PagerDutyEntity parseDBEntity(final String dbEntity)
            throws JsonMappingException, JsonProcessingException {
        log.debug("Parsing entity: {}", dbEntity);
        PagerDutyTransitionalEntity tmp = objectMapper.readValue(dbEntity, PagerDutyTransitionalEntity.class);
        return objectMapper.convertValue(tmp, tmp.getIngestionDataType().getIngestionDataTypeClass());
    }

}