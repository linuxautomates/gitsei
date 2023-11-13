package io.levelops.workflow.models.ui.configurations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CheckStatusNodeConfiguration.CheckStatusNodeConfigurationBuilder.class)
public class CheckStatusNodeConfiguration implements NodeConfiguration {

    @JsonProperty("node_id")
    String nodeId; // id of the node to check

    @JsonProperty("exit_status")
    String exitStatus; // status to match (depends on what type of node is checked, e.g. for condition -> pass or fail, for questionnaire -> pending,complete...)

    @JsonProperty("frequency")
    Frequency frequency;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Frequency.FrequencyBuilder.class)
    public static class Frequency {
        @JsonProperty("type")
        FrequencyType type;

        @JsonProperty("day_of_month")
        Integer dayOfMonth; // 1-31

        @JsonProperty("day_of_week")
        Integer dayOfWeek; // 0-6

        @JsonProperty("hour")
        Integer hour; // 0-23

        @Nullable
        @JsonProperty("cron")
        public String getCron() {
            if (type == null) {
                return null;
            }
            // <min> <hours> <day of month> <month> <day of week> [<year>]
            switch (type) {
                case DAILY:
                    return String.format("0 %d * * *", hour);
                case WEEKLY:
                    if (hour == null || dayOfWeek == null) {
                        return null;
                    }
                    return String.format("0 %d * * %d", hour, dayOfWeek);
                case MONTHLY:
                    if (hour == null || dayOfMonth == null) {
                        return null;
                    }
                    return String.format("0 %d %d * *", hour, dayOfMonth);
                default:
                    return "";
            }
        }
    }

    public enum FrequencyType {
        DAILY,
        WEEKLY,
        MONTHLY;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        @Nullable
        public static FrequencyType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(FrequencyType.class, value);
        }
    }

}
