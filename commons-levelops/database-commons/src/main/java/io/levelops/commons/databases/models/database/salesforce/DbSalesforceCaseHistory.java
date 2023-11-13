package io.levelops.commons.databases.models.database.salesforce;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.salesforce.models.SalesforceCaseHistory;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSalesforceCaseHistory.DbSalesforceCaseHistoryBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbSalesforceCaseHistory {

    public static final String UNASSIGNED = "_UNASSIGNED_";
    public static final String NO_CONTACT = "";

    @JsonProperty("case_id")
    String caseId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("bounces")
    Integer bounces;

    @JsonProperty("hops")
    Integer hops;

    public static List<DbSalesforceCaseHistory> fromSalesforceCaseHistories(
            List<SalesforceCaseHistory> salesforceCaseHistories, String integrationId) {

        List<DbSalesforceCaseHistory> dbSalesforceCaseHistories = new ArrayList<>();

        Map<String, List<SalesforceCaseHistory>> caseIdsToHistory = salesforceCaseHistories.stream()
                .collect(Collectors.groupingBy(SalesforceCaseHistory::getCaseId));

        for (Map.Entry<String, List<SalesforceCaseHistory>> caseIdToHistory : caseIdsToHistory.entrySet()) {
            List<SalesforceCaseHistory> histories = caseIdToHistory.getValue();

            Optional<SalesforceCaseHistory> caseCreateHistory = histories.stream()
                    .filter(history -> history.getField().equalsIgnoreCase("created"))
                    .findAny();

            String caseContact;
            if (caseCreateHistory.isPresent()) {
                caseContact = caseCreateHistory.get().getContact();
            } else {
                caseContact = NO_CONTACT;
            }

            int bounces = 0;
            int hops = 0;
            Set<String> contactSet = new HashSet<>();

            histories.sort(Comparator.comparing(SalesforceCaseHistory::getCreatedDate));

            SalesforceCaseHistory contactHistory = null;

            for(SalesforceCaseHistory history : histories) {
                if ("contact".equals(history.getField().toLowerCase())) {
                    if (isStringOnlyAlphabet(history.getOldValue())) {
                        contactHistory = history;
                        String contactVar = history.getOldValue();
                        if (contactVar.equalsIgnoreCase(NO_CONTACT)) {
                            contactVar = UNASSIGNED;
                        }

                        if (!UNASSIGNED.equals(contactVar)) {
                            hops += 1;
                            if (contactSet.contains(contactVar)) {
                                bounces += 1;
                            }
                        }
                        contactSet.add(contactVar);
                    }
                }
            }

            String contactVar = contactHistory != null ? contactHistory.getNewValue() : caseContact;

            if(contactVar.equalsIgnoreCase(NO_CONTACT)) {
                contactVar = UNASSIGNED;
            }
            if(!UNASSIGNED.equals(contactVar)) {
                hops += 1;
                if (contactSet.contains(contactVar)) {
                    bounces += 1;
                }
            }

            DbSalesforceCaseHistory salesforceCaseHistory = DbSalesforceCaseHistory.builder()
                    .caseId(caseIdToHistory.getKey())
                    .bounces(bounces)
                    .hops(hops)
                    .integrationId(integrationId)
                    .build();
            dbSalesforceCaseHistories.add(salesforceCaseHistory);
        }

        return dbSalesforceCaseHistories;
    }

    public static boolean isStringOnlyAlphabet(String str)
    {
        return str != null && !str.equals("") && str.matches("^[a-zA-Z ]*$");
    }
}
