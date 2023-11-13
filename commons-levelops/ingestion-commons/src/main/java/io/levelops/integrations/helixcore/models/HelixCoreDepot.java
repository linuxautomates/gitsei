package io.levelops.integrations.helixcore.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.perforce.p4java.core.IDepot;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Bean describing a HelixCoreDepot from https://www.perforce.com/manuals/p4java-javadoc/com/perforce/p4java/core/IDepot.html
 * with relevant fields
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixCoreDepot.HelixCoreDepotBuilder.class)
public class HelixCoreDepot {

    @JsonProperty
    String name;

    @JsonProperty("owner_name")
    String ownerName;

    @JsonProperty("last_modified_date")
    Date lastModifiedDate;

    @JsonProperty("description")
    String description;

    @JsonProperty
    String address;

    @JsonProperty
    DepotType type;

    public static HelixCoreDepot fromDepot(IDepot depot) {
        return HelixCoreDepot.builder()
                .name(depot.getName())
                .ownerName(depot.getOwnerName())
                .lastModifiedDate(depot.getModDate())
                .description(depot.getDescription())
                .type(DepotType.fromString(depot.getDepotType().toString()))
                .build();
    }

    public enum DepotType {
        LOCAL,
        REMOTE,
        SPEC,
        STREAM,
        ARCHIVE,
        UNLOAD,
        TANGENT,
        GRAPH,
        UNKNOWN;

        /**
         * Return a suitable HelixCoreDepot type as inferred from the passed-in
         * string, which is assumed to be the string form of a HelixCoreDepot type.
         * Otherwise return the UNKNOWN type
         */
        @JsonCreator
        @Nullable
        public static DepotType fromString(String str) {
            return EnumUtils.getEnumIgnoreCase(DepotType.class, str);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
