package io.propelo.trellis_framework.models.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

@Getter
public enum EventType {

    OU_CHANGE,
    ORG_USER_CHANGE,
    ORG_USER_INTEGRATION_CHANGE,
    ORG_USER_ATTRIBUTE_CHANGE,
    DEV_PROD_PROFILE_CHANGE,
    DEV_PROD_PROFILE_STATS_CHANGE,
    DEV_PROD_PROFILE_WEIGHTS_RATINGS_CHANGE,
    DEV_PROD_PROFILE_OU_MAPPING_CHANGE;

    @JsonCreator
    public static EventType fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(EventType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
