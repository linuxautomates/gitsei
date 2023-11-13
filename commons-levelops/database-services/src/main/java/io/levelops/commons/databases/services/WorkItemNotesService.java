package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.WorkItemNote;
import io.levelops.commons.models.DbListResponse;

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
import java.util.Set;
import java.util.UUID;

@Service
public class WorkItemNotesService extends DatabaseService<WorkItemNote> {

    @Autowired
    public WorkItemNotesService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(WorkItemDBService.class);
    }

    @Override
    public String insert(String company, WorkItemNote note) throws SQLException {

        String SQL = "INSERT INTO " + company + ".workitemnotes(body,workitemid,creator)" +
                " VALUES(?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, note.getBody());
            pstmt.setObject(2, UUID.fromString(note.getWorkItemId()));
            pstmt.setString(3, note.getCreator());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Boolean update(String company, WorkItemNote note) throws SQLException {
        String SQL = "UPDATE " + company + ".workitemnotes SET body =?, workitemid = ?, creator = ? WHERE id =?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            try {
                pstmt.setString(1, note.getBody());
                pstmt.setObject(2, UUID.fromString(note.getWorkItemId()));
                pstmt.setString(3, note.getCreator());
                pstmt.setObject(4, UUID.fromString(note.getId()));
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                return (affectedRows > 0);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    @Override
    public Optional<WorkItemNote> get(String company, String noteId) throws SQLException {
        String SQL = "SELECT * FROM " + company + ".workitemnotes WHERE id =?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(noteId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(WorkItemNote.builder()
                        .id(rs.getObject("id").toString())
                        .body(rs.getString("body"))
                        .workItemId(rs.getObject("workitemid").toString())
                        .creator(rs.getString("creator"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return Optional.empty();
    }

    public DbListResponse<WorkItemNote> list(String company, String workItemId, Integer pageNumber,
                                             Integer pageSize) throws SQLException {
        String SQL = "SELECT id,body,workitemid,creator,createdat FROM " + company + ".workitemnotes " +
                "WHERE workitemid = ? ORDER BY createdat DESC LIMIT " + pageSize + " OFFSET "
                + (pageNumber * pageSize);
        List<WorkItemNote> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + company + ".workitemnotes WHERE workitemid = ?";
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            pstmt.setObject(1, UUID.fromString(workItemId));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(WorkItemNote.builder()
                        .id(String.valueOf(rs.getObject("id")))
                        .body(rs.getString("body"))
                        .workItemId(String.valueOf(rs.getObject(
                                "workitemid")))
                        .creator(rs.getString("creator"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    pstmt2.setString(1, workItemId);
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
    public DbListResponse<WorkItemNote> list(String company, Integer pageNumber,
                                             Integer pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".workitemnotes WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setString(1, id);
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".workitemnotes(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    body VARCHAR NOT NULL,\n" +
                "    workitemid UUID REFERENCES "
                + company + ".workitems(id) ON DELETE CASCADE,\n" +
                "    creator VARCHAR NOT NULL,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sqlIndexCreation = "CREATE INDEX IF NOT EXISTS workitemnotes_workitemid_idx on "
                + company + ".workitemnotes (workitemid)";
        String sqlIndexCreation2 = "CREATE INDEX IF NOT EXISTS workitemnotes_creator_idx on "
                + company + ".workitemnotes (creator)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement index1Pstmt = conn.prepareStatement(sqlIndexCreation);
             PreparedStatement index2Pstmt = conn.prepareStatement(sqlIndexCreation2)) {
            pstmt.execute();
            index1Pstmt.execute();
            index2Pstmt.execute();
            return true;
        }
    }
}
