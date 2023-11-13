package io.levelops.api.services.ba;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TicketCategorizationSchemesService {
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @Autowired
    public TicketCategorizationSchemesService(TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService) {
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
    }

    public static void validateProfile(TicketCategorizationScheme scheme) throws BadRequestException {
        if (scheme == null) {
            return;
        }
        if (scheme.getConfig() == null) {
            return;
        }
        if (MapUtils.isEmpty(scheme.getConfig().getCategories())) {
            return;
        }
        List<Integer> duplicateIndexes = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (Map.Entry<String, TicketCategorizationScheme.TicketCategorization> e : scheme.getConfig().getCategories().entrySet()) {
            Integer index = e.getValue().getIndex();
            if (! seen.add(index)) {
                duplicateIndexes.add(index);
            }
        }
        if(CollectionUtils.isEmpty(duplicateIndexes)) {
            return;
        }
        String errorMessage = "Duplicate categories found for indexes : " + String.join(",", duplicateIndexes.stream().distinct().sorted().map(i -> String.valueOf(i)).collect(Collectors.toList()));
        throw new BadRequestException(errorMessage);
    }

    public String insert (String company, TicketCategorizationScheme scheme) throws SQLException, BadRequestException {
        validateProfile(scheme);
        return ticketCategorizationSchemeDatabaseService.insert(company, scheme);
    }

    public Boolean update (String company, UUID id, TicketCategorizationScheme scheme) throws SQLException, BadRequestException {
        validateProfile(scheme);
        return ticketCategorizationSchemeDatabaseService.update(company, scheme.toBuilder()
                .id(id.toString())
                .build());
    }
}
