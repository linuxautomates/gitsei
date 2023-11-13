package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.ScmFilterParserCommons;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class GitRepositoryService extends DatabaseService<DbRepository> {

    private final NamedParameterJdbcTemplate template;
    private ScmFilterParserCommons scmFilterParserCommons;
    private ProductsDatabaseService productsDatabaseService;

    @Autowired
    public GitRepositoryService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        scmFilterParserCommons = new ScmFilterParserCommons(productsDatabaseService);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbRepository repository) throws SQLException {

        String SQL = "INSERT INTO " + company + ".gitrepositories(integrationid,cloudid,name," +
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
        String SQL = "INSERT INTO " + company + ".gitrepositories" +
                " (integrationid, cloudid, name, ownername, ownertype, htmlurl, masterbranch," +
                "  cloudcreatedat, cloudpushedat, cloudupdatedat, size, isprivate, repotype) " +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                " ON CONFLICT (integrationid, cloudid)" +
                " DO UPDATE SET (masterbranch,cloudpushedat,cloudupdatedat,size,isprivate,repotype) = " +
                "  (EXCLUDED.masterbranch,EXCLUDED.cloudpushedat,EXCLUDED.cloudupdatedat,EXCLUDED.size," +
                "   EXCLUDED.isprivate,EXCLUDED.repotype)" +
                " RETURNING id";
        String SQL2 = "INSERT INTO " + company + ".gittechnologies(integration_id,repo_id,name) VALUES(?,?,?)" +
                " ON CONFLICT (name,repo_id,integration_id) DO UPDATE SET updated_at = ?";
        String SQL3 = "DELETE FROM " + company + ".gittechnologies WHERE updated_at < ? AND integration_id = ?";
        List<String> ids = new ArrayList<>();
        long updatedAt = (new Date()).toInstant().getEpochSecond();
        int integrationId;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt2 = conn.prepareStatement(SQL2, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement deleteStmt = conn.prepareStatement(SQL3, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0, j = 0;
            for (DbRepository repository : repositories) {
                if (StringUtils.isAnyBlank(repository.getIntegrationId(), repository.getCloudId(), repository.getName())) {
                    log.warn("Skipping insertion of malformed Github repo for tenant={}: {}", company, repository);
                    continue;
                }
                integrationId = Integer.parseInt(repository.getIntegrationId());
                pstmt.setInt(1, integrationId);
                pstmt.setString(2, repository.getCloudId());
                pstmt.setString(3, repository.getName());
                pstmt.setString(4, repository.getOwnerName());
                pstmt.setString(5, repository.getOwnerType());
                pstmt.setString(6, repository.getHtmlUrl());
                pstmt.setString(7, repository.getMasterBranch());
                pstmt.setObject(8, repository.getCloudCreatedAt(), Types.BIGINT);
                pstmt.setObject(9, repository.getCloudPushedAt(), Types.BIGINT);
                pstmt.setObject(10, repository.getCloudUpdatedAt(), Types.BIGINT);
                pstmt.setObject(11, repository.getSize(), Types.INTEGER);
                pstmt.setBoolean(12, BooleanUtils.isTrue(repository.getIsPrivate()));
                pstmt.setString(13, repository.getRepoType());
                pstmt.addBatch();
                pstmt.clearParameters();
                if (repository.getLanguages() != null) {
                    for (GitTechnology technology : repository.getLanguages()) {
                        j++;
                        pstmt2.setInt(1, Integer.parseInt(technology.getIntegrationId()));
                        pstmt2.setString(2, technology.getRepoId());
                        pstmt2.setString(3, technology.getName());
                        pstmt2.setLong(4, updatedAt);
                        pstmt2.addBatch();
                        pstmt2.clearParameters();
                        if (j % 100 == 0) {
                            pstmt2.executeBatch();
                            j = 0;
                        }
                    }
                }
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
            if (j > 0) {
                pstmt2.executeBatch();
            }
//            deleteStmt.setInt(2, integrationId);
//            deleteStmt.setLong(1, updatedAt - 86400);
        }
        return ids;
    }

    @Override
    public Boolean update(String company, DbRepository integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbRepository> get(String company, String repoId) {
        String SQL = "SELECT id,integrationid,cloudid,name,ownername,ownertype,createdat," +
                "htmlurl,masterbranch,cloudcreatedat,cloudpushedat,cloudupdatedat," +
                "size,isprivate,repotype FROM " + company + ".gitrepositories WHERE id = ? LIMIT 1";

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
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public Optional<DbRepository> get(String company, String integrationId, String cloudId) {
        String sql = "SELECT id,integrationid,cloudid,name,ownername,ownertype,createdat," +
                "htmlurl,masterbranch,cloudcreatedat,cloudpushedat,cloudupdatedat," +
                "size,isprivate,repotype FROM " + company + ".gitrepositories WHERE integrationid = ? AND cloudid = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(integrationId));
            pstmt.setString(2, cloudId);

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
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<DbRepository> listByFilter(String company, String integrationId, List<String> ids, Set<UUID> orgProductIds,
                                                     Integer pageNumber, Integer pageSize)
            throws SQLException {
        if (CollectionUtils.isNotEmpty(orgProductIds)) {
            return executeRequestWithProductIds(company, orgProductIds, pageNumber, pageSize);
        }
        List<Object> values = new ArrayList<>();
        String criteriaConditions = getCriteria(values, ids, integrationId);
        String SQL = "SELECT r.id,r.integrationid,r.cloudid,r.name,r.ownername,r.ownertype," +
                "r.htmlurl,r.masterbranch,r.cloudcreatedat,r.cloudpushedat,r.cloudupdatedat,r.createdat," +
                "r.size,r.isprivate,r.repotype FROM " + company + ".gitrepositories as r" + criteriaConditions
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<DbRepository> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM (" + SQL + ") as ct";
        Integer totCount = executeSql(pageNumber, pageSize, values, SQL, retval, countSQL);
        return DbListResponse.of(retval, totCount);
    }

    private Integer executeSql(Integer pageNumber, Integer pageSize, List<Object> values, String SQL,
                               List<DbRepository> retval, String countSQL) throws SQLException {
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
                        .cloudPushedAt(rs.getLong("cloudpushedat"))
                        .masterBranch(rs.getString("masterbranch"))
                        .cloudCreatedAt(rs.getLong("cloudcreatedat"))
                        .cloudUpdatedAt(rs.getLong("cloudupdatedat"))
                        .integrationId(rs.getString("integrationid"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return totCount;
    }

    private String getCriteria(List<Object> values, List<String> ids, String integrationId) {
        String criteria = " WHERE ";
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
        return criteria;
    }

    @SuppressWarnings("unchecked")
    private DbListResponse<DbRepository> executeRequestWithProductIds(String company, Set<UUID> orgProductIds, Integer pageNumber,
                                                                      Integer pageSize) {
        Map<Integer, Map<String, Object>> integFiltersMap;
        try {
            integFiltersMap = scmFilterParserCommons.getProductFilters(company, orgProductIds);
            List<Object> values = new ArrayList<>();
            List<DbRepository> retval = new ArrayList<>();
            List<String> listOfUnionSqls = new ArrayList<>();
            for (Integer integ : integFiltersMap.keySet()) {
                List<String> ids = (List<String>) integFiltersMap.get(integ).get("ids");
                String integrationId = String.valueOf(integ);
                String criteriaConditions = getCriteria(values, ids, integrationId);
                String unionSql = "SELECT * FROM ( SELECT r.id,r.integrationid,r.cloudid,r.name,r.ownername,r.ownertype," +
                        "r.htmlurl,r.masterbranch,r.cloudcreatedat,r.cloudpushedat,r.cloudupdatedat,r.createdat," +
                        "r.size,r.isprivate,r.repotype FROM " + company + ".gitrepositories as r" + criteriaConditions + " ) x";
                listOfUnionSqls.add(unionSql);
            }
            String SQL = String.join(" UNION ", listOfUnionSqls) + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
            ;
            String countSQL = "SELECT count(*) FROM ( " + SQL + " ) a ";
            Integer totCount = null;
            totCount = executeSql(pageNumber, pageSize, values, SQL, retval, countSQL);
            return DbListResponse.of(retval, totCount);
        } catch (SQLException throwables) {
            log.warn("Cannot fetch filters for company {}, {}", company, throwables);
        }
        return DbListResponse.of(List.of(), 0);
    }

    public List<String> getOrganizations(String company, String integrationId) {
        String SQL = "SELECT DISTINCT(ownername) FROM " + company + ".gitrepositories WHERE integrationid = :integrationid "
                + "AND ownertype = :ownertype";
        Map<String, Object> params = Map.of("integrationid", Integer.parseInt(integrationId),
                "ownertype", "organization");
        return template.query(SQL, params, orgListRowMapper());
    }

    public RowMapper<String> orgListRowMapper() {
        return (rs, rowNumber) -> rs.getString("ownername");
    }


    @Override
    public DbListResponse<DbRepository> list(String company, Integer pageNumber,
                                             Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, null, pageNumber, pageSize);
    }

    public Boolean deleteForIntegration(String company, String integrationId) throws SQLException {
        String SQL = "DELETE FROM " + company + ".gitrepositories WHERE integrationid = ?";

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
        String SQL = "DELETE FROM " + company + ".gitrepositories WHERE id = ?";

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
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".gitrepositories(\n" +
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
                "        repotype VARCHAR, \n" +
                "        isprivate BOOLEAN NOT NULL, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";
        String sqlIndex1Creation = "CREATE INDEX IF NOT EXISTS gitrepositories_integrationid_idx on "
                + company + ".gitrepositories (integrationid)";
        String sqlIndex2Creation = "CREATE INDEX IF NOT EXISTS gitrepositories_cloudid_idx on "
                + company + ".gitrepositories (cloudid)";
        String sqlIndex3Creation = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_gitrepositories_compound_idx on "
                + company + ".gitrepositories (integrationid,cloudid)";

        String SQL2 = "CREATE TABLE IF NOT EXISTS " + company + ".gittechnologies(\n" +
                "       id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "       repo_id VARCHAR NOT NULL, \n" +
                "       name VARCHAR NOT NULL, \n" +
                "       integration_id INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE, \n" +
                "       updated_at BIGINT DEFAULT extract(epoch from now()),\n" +
                "       created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                "       UNIQUE (name,repo_id,integration_id)" +
                "    )";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(SQL2);
             PreparedStatement index1Pstmt = conn.prepareStatement(sqlIndex1Creation);
             PreparedStatement index2Pstmt = conn.prepareStatement(sqlIndex2Creation);
             PreparedStatement index3Pstmt = conn.prepareStatement(sqlIndex3Creation)) {
            pstmt.execute();
            pstmt2.execute();
            index1Pstmt.execute();
            index2Pstmt.execute();
            index3Pstmt.execute();
            return true;
        }
    }

}