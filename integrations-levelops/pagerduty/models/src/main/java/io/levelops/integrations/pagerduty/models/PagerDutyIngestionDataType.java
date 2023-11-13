package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.levelops.ingestion.models.IngestionDataType;

import org.apache.commons.lang3.EnumUtils;

public enum PagerDutyIngestionDataType implements IngestionDataType<PagerDutyEntity, PagerDutyResponse> {
    ALERT("alert", "alerts", PagerDutyAlert.class, PagerDutyAlertsPage.class),
    INCIDENT("incident", "incidents", PagerDutyIncident.class, PagerDutyIncidentsPage.class),
    LOG_ENTRY("log_entry", "log_entries", PagerDutyLogEntry.class, PagerDutyLogEntriesPage.class),
    SERVICE("service", "services", PagerDutyService.class, PagerDutyServicesPage.class),
    USER("user", "users", PagerDutyUser.class, PagerDutyUsersPage.class);

    private final String singular;
    private final String plural;
    private final Class<? extends PagerDutyEntity> singularClass;
    private final Class<? extends PagerDutyResponse> pluralClass;

    private PagerDutyIngestionDataType(final String singular, final String plural, 
            Class<? extends PagerDutyEntity> singularClass, 
            Class<? extends PagerDutyResponse> pluralClass){
        this.singular = singular;
        this.plural = plural;
        this.singularClass = singularClass;
        this.pluralClass = pluralClass;
    }

    @Override
    public String getIngestionDataType() {
        return singular;
    }

    @Override
    public String getIngestionPluralDataType() {
        return plural;
    }

    @Override
    public Class<? extends PagerDutyEntity> getIngestionDataTypeClass() {
        return singularClass;
    }

    @Override
    public Class<? extends PagerDutyResponse> getIngestionPluralDataTypeClass() {
        return pluralClass;
    }

    @JsonCreator
    public static PagerDutyIngestionDataType fromString(final String value){
        PagerDutyIngestionDataType type = EnumUtils.getEnumIgnoreCase(PagerDutyIngestionDataType.class, value);
        if(type != null) {
            return type;
        }
        for (PagerDutyIngestionDataType item:values()) {
            if(item.plural.equals(value) || item.singular.equals(value)){
                return item;
            }
        }
        return null;
    }

}