package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;

import java.sql.SQLException;

public class TicketCategorizationSchemeDatabaseServiceTestUtils {
    public static TicketCategorizationScheme createTicketCategorizationScheme(TicketCategorizationSchemeDatabaseService dbService, String company, int i) throws SQLException {
        TicketCategorizationScheme ticketCategorizationScheme = TicketCategorizationScheme.builder()
                .name("scheme-" + i)
                .build();
        String ticketCategorySchemeId = dbService.insert(company, ticketCategorizationScheme);
        return  ticketCategorizationScheme.toBuilder().id(ticketCategorySchemeId).build();
    }
}
