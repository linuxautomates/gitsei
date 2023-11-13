package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.models.DbListResponse;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public abstract class DatabaseService<V> {
    static final String LEVELOPS_INVENTORY_SCHEMA = "_levelops";


    protected DataSource dataSource;

    protected DatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns True if the schema is tenant specific.
     * Needed by internal-api for setup purposes.
     */
    public boolean isTenantSpecific() {
        return true;
    }

    public SchemaType getSchemaType() {
        return SchemaType.TENANT_SPECIFIC;
    }

    /**
     * Returns set of tables this service depends on.
     * Needed by internal-api for setup purposes.
     */
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Collections.emptySet();
    }

    public abstract String insert(String company, V t) throws SQLException;

    // TODO move every 'insert' to 'insertAndReturnId' and rename this to 'insert'
    public Optional<String> insertAndReturnId(String company, V t) throws SQLException {
        return Optional.ofNullable(insert(company, t));
    }

    public abstract Boolean update(String company, V t) throws SQLException;

    public abstract Optional<V> get(String company, String param) throws SQLException;

    public abstract DbListResponse<V> list(String company, Integer pageNumber, Integer pageSize) throws SQLException;

    public abstract Boolean delete(String company, String id) throws SQLException;

    public abstract Boolean ensureTableExistence(String company) throws SQLException;

    /**
     * @deprecated use DatabaseSchemaService
     */
    @Deprecated
    public Boolean ensureSchemaExistence(String company) throws SQLException {
        if (StringUtils.isEmpty(company)
                || !(LEVELOPS_INVENTORY_SCHEMA.equals(company) || company.matches("[a-zA-Z0-9]+"))) {
            return false;
        }

        String SQL = "CREATE SCHEMA IF NOT EXISTS " + company;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.NO_GENERATED_KEYS)) {
            pstmt.execute();
            return true;
        }
    }

    @Getter
    public enum SchemaType {
        TENANT_SPECIFIC(null, true),
        LEVELOPS_INVENTORY_SCHEMA("_levelops", false),
        FS_CONTROLLER_SCHEMA("_levelops_faceted_search_controller", false),
        ST_CONTROLLER_SCHEMA("_levelops_scheduled_tasks_controller", false),
        CICD_TASK_SCHEMA("_levelops_cicd_pre_process", false),
        ETL_SCHEMA("_levelops_etl", false),
        TRELLIS_SCHEMA("_levelops_trellis", false),
        ATLASSIAN_CONNECT_SCHEMA("_levelops_atlassian_connect", false);
        private final String schemaName;
        private final boolean tenantSpecific;


        SchemaType(String schemaName, boolean tenantSpecific) {
            this.schemaName = schemaName;
            this.tenantSpecific = tenantSpecific;
        }

        public static SchemaType fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(SchemaType.class, st);
        }

        public String getSchemaName() {
            if (this.tenantSpecific) {
                throw new RuntimeException("Tenant specific schema does not have a name");
            }
            return schemaName;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

}
