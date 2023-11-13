package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.TokenData;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class TokenDataService extends DatabaseService<Token> {

    private final ObjectMapper objectMapper;

    @Autowired
    public TokenDataService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, Token token) throws SQLException {

        String SQL = "INSERT INTO " + company + ".tokens(integrationid,tokentype,tokendata) "
                + "VALUES(?,?,to_json(?::json))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, Integer.parseInt(token.getIntegrationId()));
            pstmt.setString(2, token.getTokenData().getType());
            pstmt.setString(3, objectMapper.writeValueAsString(token.getTokenData()));

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
        } catch (JsonProcessingException jpe) {
            throw new SQLException("TokenDataService - Failed to convert tokendata to string.");
        }
        return null;
    }

    @Override
    public Boolean update(String company, Token token) throws SQLException {
        String SQL = "UPDATE " + company + ".tokens SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<String> values = new ArrayList<>();
        if (token.getTokenData() != null && StringUtils.isNotEmpty(token.getTokenData().getType())) {
            updates = "tokentype = ?";
            values.add(token.getTokenData().getType());
        }
        if (ObjectUtils.isNotEmpty(token.getTokenData())) {
            updates = StringUtils.isEmpty(updates) ?
                    "tokendata = to_json(?::json)" : updates + ", tokendata = to_json(?::json)";
            try {
                values.add(objectMapper.writeValueAsString(token.getTokenData()));
            } catch (JsonProcessingException e) {
                throw new SQLException("TokendataService couldnt convert token data to json string.");
            }
        }
        SQL = SQL + updates + condition;
        //no updates
        if (values.size() == 0) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setString(i, values.get(i - 1));
            }
            pstmt.setInt(values.size() + 1, Integer.parseInt(token.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<Token> get(String company, String tokenDataId) {
        String SQL = "SELECT id,integrationid,tokentype,tokendata,createdat FROM " + company
                + ".tokens WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(tokenDataId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                try {
                    return Optional.of(Token.builder()
                            .id(rs.getString("id"))
                            .integrationId(rs.getString("integrationid"))
                            .tokenData(objectMapper.readValue(rs.getString("tokendata"), TokenData.class))
                            .createdAt(rs.getLong("createdat"))
                            .build());
                } catch (IOException e) {
                    throw new SQLException("Tokendata service couldnt convert string to token data.");
                }
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<Token> listByIntegration(String company, String integrationId,
                                                   Integer pageNumber, Integer pageSize)
            throws SQLException {
        String SQL = "SELECT id,integrationid,tokentype,tokendata,createdat FROM "
                + company + ".tokens WHERE integrationid = ? LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        List<Token> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + company + ".tokens WHERE integrationid = ?";
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            pstmt.setInt(1, Integer.parseInt(integrationId));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(Token.builder()
                        .id(rs.getString("id"))
                        .integrationId(rs.getString("integrationid"))
                        .tokenData(objectMapper.readValue(rs.getString("tokendata"), TokenData.class))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    pstmt2.setInt(1, Integer.parseInt(integrationId));
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        } catch (IOException e) {
            throw new SQLException("Tokendata service couldnt convert string to token data.");
        }
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public DbListResponse<Token> list(String company, Integer pageNumber,
                                      Integer pageSize) {
        throw new UnsupportedOperationException();
    }

    public Boolean deleteByIntegration(String company, String integrationid) throws SQLException {
        String SQL = "DELETE FROM " + company + ".tokens WHERE integrationid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(integrationid));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".tokens WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".tokens(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        integrationid INTEGER REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE, \n" +
                "        tokentype VARCHAR(50) NOT NULL\n," +
                "        tokendata JSONB NOT NULL\n," +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";
        String sqlIndexCreation = "CREATE INDEX IF NOT EXISTS tokens_integrationid_idx on "
                + company + ".tokens (integrationid)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement indexPstmt = conn.prepareStatement(sqlIndexCreation)) {
            pstmt.execute();
            indexPstmt.execute();
            return true;
        }
    }

}