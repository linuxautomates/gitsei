package io.levelops.internal_api.config;

import io.levelops.commons.databases.services.CiCdPreProcessTaskService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Log4j2
@Component
public class CiCdSchemaInitialization {
    private final DataSource dataSource;
    private final List<DatabaseService<?>> databaseServices;
    private final CiCdPreProcessTaskService ciCdPreProcessTaskService;

    @Autowired
    public CiCdSchemaInitialization(DataSource dataSource, List<DatabaseService<?>> databaseServices, CiCdPreProcessTaskService ciCdPreProcessTaskService) {
        this.dataSource = dataSource;
        this.databaseServices = databaseServices;
        this.ciCdPreProcessTaskService = ciCdPreProcessTaskService;
    }
    @PostConstruct
    public void initializeDatabase() throws SQLException {
        log.info("Starting initializing db");
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(DatabaseService.SchemaType.CICD_TASK_SCHEMA.getSchemaName());
        ciCdPreProcessTaskService.ensureTableExistence(null);
        log.info("Completed initializing CiCd Task schema db");
    }
}