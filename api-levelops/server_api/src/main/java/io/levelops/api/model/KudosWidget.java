package io.levelops.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = KudosWidget.KudosWidgetBuilderImpl.class)
public class KudosWidget {
        UUID widgetId;
        Integer position;
        Integer size;
        Map<String, Object> data;
        Instant createdAt;

        @JsonPOJOBuilder(withPrefix = "")
        public final static class KudosWidgetBuilderImpl extends KudosWidgetBuilder {

        }
}
