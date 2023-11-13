package io.levelops.integrations.checkmarx.models.cxsast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxSastIterativeScanQuery.CxSastIterativeScanQueryBuilder.class)
public class CxSastIterativeScanQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("teamId")
    String teamId;

    @JsonProperty("projectName")
    String projectName;

    @JsonProperty("fetchOnce")
    boolean fetchOnce;

    @JsonProperty("projectId")
    String projectId;

    @JsonProperty("scanStatus")
    String scanStatus;

    @JsonProperty("last")
    Integer last;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;
}
