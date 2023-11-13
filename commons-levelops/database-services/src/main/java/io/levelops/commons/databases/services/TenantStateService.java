package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TenantState;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class TenantStateService extends DatabaseService<TenantState> {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TenantStateService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        // TODO Auto-generated method stub
        return Set.of(DashboardWidgetService.class);
    }

    @Override
    public String insert(String company, TenantState state) throws SQLException {
        String retVal = null;
        String SQL = "INSERT INTO " + company + ".tenantstates(state,createdat) VALUES(?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, state.getState().toString().toUpperCase());
            pstmt.setLong(2, state.getCreatedAt());
            pstmt.executeUpdate();
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    retVal = rs.getString(1);
                }
            }
        }
        return retVal;
    }

    //THIS UPDATES by CONFIG NAME
    @Override
    public Boolean update(String company, TenantState state) throws SQLException {
        String SQL = "UPDATE " + company + ".tenantstates SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        //empty string is valid description
        if (StringUtils.isNotEmpty(state.getState().toString())) {
            updates = StringUtils.isEmpty(updates) ? "state = ?" : updates + ", state = ?";
            values.add(state.getState().toString());
        }
        //nothing to update.
        if (values.size() == 0) {
            return false;
        }
        SQL = SQL + updates + condition;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
            }
            pstmt.setInt(values.size() + 1, Integer.parseInt(state.getId()));
            pstmt.executeUpdate();
        }
        return false;
    }

    @Override
    public Optional<TenantState> get(String company, String stateId) {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<TenantState> listByFilter(String company, String state, Integer pageNumber,
                                                     Integer pageSize, Boolean withoutPagination) throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(state)) {
            criteria += "state = ? ";
            values.add(state.toUpperCase());
        }
        if (values.size() == 0) {
            criteria = "";
        }
        String limitString = withoutPagination ? "" : " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<TenantState> retval = new ArrayList<>();
        String SQL = "SELECT id,state,createdat FROM " + company + ".tenantstates "
                + criteria + "ORDER BY createdat DESC " + limitString;
        String countSQL = "SELECT COUNT(*) FROM ( SELECT id,state FROM "
                + company + ".tenantstates " + criteria + ") AS d";
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            int i = 1;
            for (Object obj : values) {
                pstmt.setObject(i, obj);
                pstmt2.setObject(i++, obj);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(TenantState.builder()
                        .id(rs.getString("id"))
                        .state(TenantState.State.valueOf(rs.getString("state")))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = withoutPagination ? retval.size() : retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public DbListResponse<TenantState> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, null, pageNumber, pageSize,false);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".tenantstates(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        state VARCHAR UNIQUE NOT NULL, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )",

                "CREATE INDEX IF NOT EXISTS tenant_states_state_idx ON " + company + ".tenantstates(state)");
        ddl.forEach(statement -> template.getJdbcTemplate().execute(statement));
        return true;
    }
}