package io.levelops.commons.databases.models.database.salesforce;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.salesforce.models.SalesforceCase;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;

@Value
@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSalesforceCase.DbSalesforceCaseBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbSalesforceCase {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String UNASSIGNED = "_UNASSIGNED_";
    public static final String NO_CONTACT = "";

    @JsonProperty("case_id")
    String caseId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("account_name")
    String accountName;

    @JsonProperty("subject")
    String subject;

    @JsonProperty("contact")
    String contact;

    @JsonProperty("creator")
    String creator;

    @JsonProperty("is_closed")
    Boolean isClosed;

    @JsonProperty("is_deleted")
    Boolean isDeleted;

    @JsonProperty("is_escalated")
    Boolean isEscalated;

    @JsonProperty("origin")
    String origin;

    @JsonProperty("status")
    String status;

    @JsonProperty("type")
    String type;

    @JsonProperty("case_number")
    String caseNumber;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("reason")
    String reason;

    @JsonProperty("sf_created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date createdAt;

    @JsonProperty("sf_modified_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastModifiedAt;

    @JsonProperty("resolved_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date resolvedAt;

    @JsonProperty("ingested_at")
    Long ingestedAt;

    @JsonProperty("bounces")
    Integer bounces;

    @JsonProperty("hops")
    Integer hops;

    public static DbSalesforceCase fromSalesforceCase(SalesforceCase salesforceCase, String integrationId, Date fetchTime) {
        String contactVar = salesforceCase.getContactEmail();
        if (contactVar.equalsIgnoreCase(NO_CONTACT)) {
            contactVar = UNASSIGNED;
        }

        return DbSalesforceCase.builder()
                .caseId(salesforceCase.getId())
                .integrationId(integrationId)
                .accountName(salesforceCase.getAccountName())
                .subject(salesforceCase.getSubject())
                .contact(contactVar)
                .caseNumber(salesforceCase.getCaseNumber())
                .creator(salesforceCase.getCreatedBy().getEmail())
                .isClosed(salesforceCase.getIsClosed())
                .isDeleted(salesforceCase.getIsDeleted())
                .isEscalated(salesforceCase.getIsEscalated())
                .origin(salesforceCase.getOrigin())
                .status(salesforceCase.getStatus().toUpperCase())
                .type(salesforceCase.getType().toUpperCase())
                .priority(salesforceCase.getPriority().toUpperCase())
                .reason(salesforceCase.getReason())
                .createdAt(salesforceCase.getCreatedDate())
                .lastModifiedAt(salesforceCase.getLastModifiedDate())
                .resolvedAt(salesforceCase.getClosedDate())
                .ingestedAt(DateUtils.truncate(fetchTime, Calendar.DATE).toInstant().getEpochSecond())
                .bounces(0)
                .hops(0)
                .build();
    }
}
