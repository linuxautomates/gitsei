package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
import java.util.stream.Collectors;

@Log4j2
@Service
public class BitbucketRepositoryService extends DatabaseService<DbRepository> {
    private ObjectMapper mapper;

    @Autowired
    public BitbucketRepositoryService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.mapper = mapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbRepository repository) throws SQLException {

        String SQL = "INSERT INTO " + company + ".bitbucketrepositories(integrationid,cloudid,name," +
                "ownername,ownertype,htmlurl,masterbranch,cloudcreatedat,cloudpushedat,cloudupdatedat," +
                "size,isprivate,repotype) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, Integer.parseInt(repository.getIntegrationId()));
            pstmt.setString(2, repository.getCloudId());
            pstmt.setString(3, repository.getName());
            pstmt.setString(4, repository.getOwnerName());
            pstmt.setString(5, repository.getOwnerType());
            pstmt.setString(6, repository.getHtmlUrl());
            pstmt.setString(7, repository.getMasterBranch());
            pstmt.setLong(8, repository.getCloudCreatedAt());
            pstmt.setLong(9, repository.getCloudPushedAt());
            pstmt.setLong(10, repository.getCloudUpdatedAt());
            pstmt.setLong(11, repository.getSize());
            pstmt.setBoolean(12, repository.getIsPrivate());
            pstmt.setString(13, repository.getRepoType());

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

    public List<String> batchUpsert(String company, List<DbRepository> repositories) throws SQLException {
        String SQL = "INSERT INTO " + company + ".bitbucketrepositories(integrationid,cloudid,name," +
                "ownername,ownertype,htmlurl,masterbranch,cloudcreatedat,cloudpushedat,cloudupdatedat," +
                "size,isprivate,repotype) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (integrationid, cloudid)" +
                "DO UPDATE SET (masterbranch,cloudpushedat,cloudupdatedat,size,isprivate,repotype) = " +
                "(EXCLUDED.masterbranch,EXCLUDED.cloudpushedat,EXCLUDED.cloudupdatedat,EXCLUDED.size," +
                "EXCLUDED.isprivate,EXCLUDED.repotype) RETURNING id";
        List<String> ids = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            for (DbRepository repository : repositories) {
                pstmt.setInt(1, Integer.parseInt(repository.getIntegrationId()));
                pstmt.setString(2, repository.getCloudId());
                pstmt.setString(3, repository.getName());
                pstmt.setString(4, repository.getOwnerName());
                pstmt.setString(5, repository.getOwnerType());
                pstmt.setString(6, repository.getHtmlUrl());
                pstmt.setString(7, repository.getMasterBranch());
                pstmt.setLong(8, repository.getCloudCreatedAt());
                pstmt.setLong(9, repository.getCloudPushedAt());
                pstmt.setLong(10, repository.getCloudUpdatedAt());
                pstmt.setLong(11, repository.getSize());
                pstmt.setBoolean(12, repository.getIsPrivate());
                pstmt.setString(13, repository.getRepoType());
                pstmt.addBatch();
                pstmt.clearParameters();
                i++;
                if (i % 100 == 0) {
                    pstmt.executeBatch();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }
            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        return ids;
    }

    public void batchUpdateCommitCts(String company, List<DbRepository> repositories)
            throws SQLException {
        String SQL = "UPDATE " + company + ".bitbucketrepositories SET commitct30 = ? WHERE cloudid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            int i = 0;
            for (DbRepository repository : repositories) {
                pstmt.setInt(1, repository.getCommitCount30d());
                pstmt.setString(2, repository.getCloudId());

                pstmt.addBatch();
                pstmt.clearParameters();
                i++;
                if (i % 100 == 0) {
                    pstmt.executeBatch();
                }

            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
            }
        }
    }

    @Override
    public Boolean update(String company, DbRepository integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbRepository> get(String company, String repoId) {
        String SQL = "SELECT id,integrationid,cloudid,name,ownername,ownertype,createdat," +
                "htmlurl,masterbranch,cloudcreatedat,cloudpushedat,cloudupdatedat," +
                "size,isprivate,repotype,commitct30 FROM " + company + ".bitbucketrepositories " +
                "WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(repoId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(DbRepository.builder()
                        .id(rs.getString("id"))
                        .integrationId(rs.getString("integrationid"))
                        .cloudId(rs.getString("cloudid"))
                        .name(rs.getString("name"))
                        .ownerName(rs.getString("ownername"))
                        .ownerType(rs.getString("ownertype"))
                        .htmlUrl(rs.getString("htmlurl"))
                        .masterBranch(rs.getString("masterbranch"))
                        .cloudCreatedAt(rs.getLong("cloudcreatedat"))
                        .cloudPushedAt(rs.getLong("cloudpushedat"))
                        .cloudUpdatedAt(rs.getLong("cloudupdatedat"))
                        .size(rs.getInt("size"))
                        .createdAt(rs.getLong("createdat"))
                        .isPrivate(rs.getBoolean("isprivate"))
                        .repoType(rs.getString("repotype"))
                        .commitCount30d(rs.getInt("commitct30"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<DbRepository> listByFilter(String company, String integrationId, List<String> ids,
                                                     Integer pageNumber, Integer pageSize)
            throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(integrationId)) {
            criteria += "r.integrationid = ? ";
            values.add(Integer.parseInt(integrationId));
        }
        if (!CollectionUtils.isEmpty(ids)) {
            criteria += (values.size() == 0) ? "r.id = ANY(?) " : "AND r.id = ANY(?) ";
            values.add(ids.stream().map(Integer::parseInt).collect(Collectors.toList()));
        }

        if (values.size() == 0) {
            criteria = " ";
        }

        String SQL = "SELECT r.id,r.commitct30,r.integrationid,r.cloudid,r.name,r.ownername,r.ownertype," +
                "r.htmlurl,r.masterbranch,r.cloudcreatedat,r.cloudpushedat,r.cloudupdatedat,r.createdat," +
                "r.size,r.isprivate,r.repotype FROM " + company + ".bitbucketrepositories as r" + criteria
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<DbRepository> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM ( SELECT r.id,r.integrationid FROM " + company
                + ".bitbucketrepositories as r" + criteria + ") as ct";
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {

            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                if (obj instanceof List) {
                    obj = conn.createArrayOf("int", ((List) obj).toArray());
                }
                pstmt.setObject(i, obj);
                pstmt2.setObject(i, obj);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(DbRepository.builder()
                        .id(rs.getString("id"))
                        .size(rs.getInt("size"))
                        .name(rs.getString("name"))
                        .htmlUrl(rs.getString("htmlurl"))
                        .cloudId(rs.getString("cloudid"))
                        .repoType(rs.getString("repotype"))
                        .createdAt(rs.getLong("createdat"))
                        .ownerName(rs.getString("ownername"))
                        .ownerType(rs.getString("ownertype"))
                        .isPrivate(rs.getBoolean("isprivate"))
                        .commitCount30d(rs.getInt("commitct30"))
                        .cloudPushedAt(rs.getLong("cloudpushedat"))
                        .masterBranch(rs.getString("masterbranch"))
                        .cloudCreatedAt(rs.getLong("cloudcreatedat"))
                        .cloudUpdatedAt(rs.getLong("cloudupdatedat"))
                        .integrationId(rs.getString("integrationid"))
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
    public DbListResponse<DbRepository> list(String company, Integer pageNumber,
                                             Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, pageNumber, pageSize);
    }

    public Boolean deleteForIntegration(String company, String integrationId) throws SQLException {
        String SQL = "DELETE FROM " + company + ".bitbucketrepositories WHERE integrationid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(integrationId));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".bitbucketrepositories WHERE id = ?";

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
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".bitbucketrepositories(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        cloudid VARCHAR NOT NULL, \n" +
                "        name VARCHAR NOT NULL, \n" +
                "        integrationid INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE, \n" +
                "        ownername VARCHAR, \n" +
                "        ownertype VARCHAR, \n" +
                "        htmlurl VARCHAR, \n" +
                "        masterbranch VARCHAR, \n" +
                "        cloudcreatedat BIGINT, \n" +
                "        cloudpushedat BIGINT, \n" +
                "        cloudupdatedat BIGINT, \n" +
                "        size INTEGER, \n" +
                "        commitct30 INTEGER, \n" +
                "        repotype VARCHAR, \n" +
                "        isprivate BOOLEAN NOT NULL, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";
        String sqlIndex1Creation = "CREATE INDEX IF NOT EXISTS bitbucketrepositories_integrationid_idx on "
                + company + ".bitbucketrepositories (integrationid)";
        String sqlIndex2Creation = "CREATE INDEX IF NOT EXISTS bitbucketrepositories_cloudid_idx on "
                + company + ".bitbucketrepositories (cloudid)";
        String sqlIndex3Creation = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_bitbucketrepositories_compound_idx on "
                + company + ".bitbucketrepositories (integrationid,cloudid)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement index1Pstmt = conn.prepareStatement(sqlIndex1Creation);
             PreparedStatement index2Pstmt = conn.prepareStatement(sqlIndex2Creation);
             PreparedStatement index3Pstmt = conn.prepareStatement(sqlIndex3Creation)) {
            pstmt.execute();
            index1Pstmt.execute();
            index2Pstmt.execute();
            index3Pstmt.execute();
            return true;
        }
    }

}
