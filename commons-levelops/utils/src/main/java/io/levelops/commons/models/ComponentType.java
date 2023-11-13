package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum ComponentType {
    NONE("none"),
    UNKNOWN("unknown"),
    INTEGRATION("integration"),
    PLUGIN_RESULT("plugins"),
    WORK_ITEM("workitems"),
    SMART_TICKET("tickets"),
    ASSESSMENT("quiz"),
    TRIAGE_RULES_MATCHED("triage_rules_matched"),
    DASHBOARD_REPORT("dashboards"),
    CUSTOM("custom"); // for customer-initiated stuff

    private String storageName;

    private ComponentType(final String storageName){
        this.storageName = storageName;
    }

    @JsonCreator
    @Nullable
    public static final ComponentType fromString(final String componentType){
        if(Strings.isBlank(componentType)){
            return NONE;
        }
        var e = EnumUtils.getEnumIgnoreCase(ComponentType.class, componentType);
        if (e != null){
            return e;
        }
        for (ComponentType t:ComponentType.values()) {
            if (t.getStorageName().equalsIgnoreCase(componentType)){
                return t;
            }
        }
        return UNKNOWN;
    }

    @Override
    @JsonValue
    public String toString(){
        return super.toString().toLowerCase();
    }
}