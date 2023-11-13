package io.levelops.commons.services.business_alignment.es.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(builderMethodName = "hiddenBuilder")
public class FTEResultMergeRequest {
    private final String ticketCategory;
    private final List<FTEPartial> forCategory;
    private final List<FTEPartial> allTickets;

    public static FTEResultMergeRequestBuilder builder(String ticketCategory, List<FTEPartial> forCategory, List<FTEPartial> allTickets) {
        return hiddenBuilder().ticketCategory(ticketCategory).forCategory(forCategory).allTickets(allTickets);
    }
}
