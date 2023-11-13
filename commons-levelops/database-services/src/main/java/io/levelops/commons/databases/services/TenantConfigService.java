package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

@Log4j2
@Service
public class TenantConfigService extends DatabaseService<TenantConfig> {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TenantConfigService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        // TODO Auto-generated method stub
        return Set.of(DashboardWidgetService.class);
    }

    @Override
    public String insert(String company, TenantConfig config) throws SQLException {
        String retVal = null;
        String SQL = "INSERT INTO " + company + ".tenantconfigs(name,value) VALUES(?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, config.getName().toUpperCase());
            pstmt.setString(2, config.getValue());
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
    public Boolean update(String company, TenantConfig config) throws SQLException {
        String SQL = "UPDATE " + company + ".tenantconfigs SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        //empty string is valid description
        if (StringUtils.isNotEmpty(config.getValue())) {
            updates = StringUtils.isEmpty(updates) ? "value = ?" : updates + ", value = ?";
            values.add(config.getValue());
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
            pstmt.setInt(values.size() + 1, Integer.parseInt(config.getId()));
            pstmt.executeUpdate();
        }
        return false;
    }

    @Override
    public Optional<TenantConfig> get(String company, String configId) {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<TenantConfig> listByFilter(String company, String name, Integer pageNumber,
                                                     Integer pageSize) throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(name)) {
            criteria += "name = ? ";
            values.add(name.toUpperCase());
        }
        if (values.size() == 0) {
            criteria = "";
        }
        List<TenantConfig> retval = new ArrayList<>();
        String SQL = "SELECT id,name,value FROM " + company + ".tenantconfigs "
                + criteria + "ORDER BY createdat DESC LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM ( SELECT id,name FROM "
                + company + ".tenantconfigs " + criteria + ") AS d";
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
                retval.add(TenantConfig.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .value(rs.getString("value"))
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
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public DbListResponse<TenantConfig> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".tenantconfigs(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        name VARCHAR UNIQUE NOT NULL, \n" +
                "        value VARCHAR NOT NULL, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )",

                "CREATE INDEX IF NOT EXISTS tenant_configs_name_idx ON " + company + ".tenantconfigs(name)");
        ddl.forEach(statement -> template.getJdbcTemplate().execute(statement));
        return true;
    }
}