package io.levelops.commons.databases.services;

import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class StateDBService extends DatabaseService<State>{
    @Autowired
    public StateDBService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String insert(String company, State t) throws SQLException {
        String SQL = "INSERT INTO " + company + ".states(name) VALUES(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            try {
                pstmt.setString(1, t.getName());
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        while (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return null;
    }

    @Override
    public Boolean update(String company, State t) throws SQLException {
        String SQL = "UPDATE " + company + ".states SET name =? WHERE id =?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            try {
                pstmt.setString(1, t.getName());
                pstmt.setInt(2, t.getId());
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                return (affectedRows > 0);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    @Override
    public Optional<State> get(String company, String id) throws SQLException {
        String SQL = "SELECT id,name,created_at FROM " + company + ".states WHERE id =?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(id));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(State.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .createdAt(rs.getLong("created_at"))
                        .build());
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return Optional.empty();
    }

    public State getStateByName(String company, String name) throws SQLException {
        DbListResponse<State> dbListResponse = listByFilter(company, name, null,0, 10);
        if(dbListResponse == null){
            throw new SQLException("Cannot find state by name : " + name);
        }
        if(dbListResponse.getTotalCount() != 1){
            throw new SQLException("Cannot find state by name : " + name);
        }
        return dbListResponse.getRecords().get(0);
    }

    @Override
    public DbListResponse<State> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        String SQL = "SELECT id,name,created_at FROM " + company + ".states LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<State> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + company + ".states";
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(State.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .createdAt(rs.getLong("created_at"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
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

    public DbListResponse<State> listByFilter(String company, String exactName, String partialName, Integer pageNumber, Integer pageSize) throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(exactName)) {
            criteria += "name ILIKE ? ";
            values.add(exactName);
        } else if (StringUtils.isNotEmpty(partialName)) {
            criteria += "name ILIKE ? ";
            values.add(partialName + "%");
        }
        if (values.size() == 0) {
            criteria = "";
        }

        String sqlBase = "SELECT id,name,created_at FROM " + company + ".states " + criteria;

        String SQL = sqlBase + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM ( " + sqlBase + " ) x";
        List<State> retval = new ArrayList<>();
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(State.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .createdAt(rs.getLong("created_at"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
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
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".states WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    private void insertDefaultData(String company, Connection conn) throws SQLException {
        String sql = "INSERT INTO " + company + ".states (name) VALUES (?) ON CONFLICT(name) DO NOTHING";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for(WorkItem.ItemStatus st : WorkItem.ItemStatus.values()){
                pstmt.setString(1, st.toString());
                pstmt.addBatch();
                pstmt.clearParameters();
            }
            pstmt.executeBatch();
        }

    }
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".states(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    name VARCHAR, \n" +
                "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                "    CONSTRAINT unq_states_name UNIQUE(name)\n" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
            insertDefaultData(company, conn);
            return true;
        }
    }
}
