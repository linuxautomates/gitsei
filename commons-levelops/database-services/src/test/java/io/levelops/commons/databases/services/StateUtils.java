package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.State;

import java.sql.SQLException;

public class StateUtils {
    public static State createOpenState(StateDBService stateDBService, String company) throws SQLException {
        State st = State.builder().name("Open").build();
        String stateId = stateDBService.insert(company, st);
        return st.toBuilder().id(Integer.parseInt(stateId)).build();
    }
}
