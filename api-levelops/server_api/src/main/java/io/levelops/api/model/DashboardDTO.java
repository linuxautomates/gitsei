package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import java.util.Set;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = DashboardDTO.DashboardDTOBuilderImpl.class)
public class DashboardDTO extends Dashboard{
    @JsonProperty("category")
    Set<UUID> category;

    @JsonPOJOBuilder(withPrefix = "")
    public final static class DashboardDTOBuilderImpl extends DashboardDTOBuilder {}
}
