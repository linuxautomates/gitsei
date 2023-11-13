package io.levelops.commons.databases.models.database.kudos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = DBKudosWidget.DBKudosWidgetBuilderImpl.class)
public class DBKudosWidget {
    UUID id;
    UUID widgetId;
    UUID screenshotId;
    Integer position;
    Integer size;
    Map<String, Object> data;

    @JsonPOJOBuilder(withPrefix = "")
    static final class DBKudosWidgetBuilderImpl extends DBKudosWidget.DBKudosWidgetBuilder {

    }
}
