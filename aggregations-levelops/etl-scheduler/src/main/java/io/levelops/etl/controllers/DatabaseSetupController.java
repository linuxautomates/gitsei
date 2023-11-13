package io.levelops.etl.controllers;

import io.levelops.aggregations_shared.database.DatabaseService;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;

@Log4j2
@RestController
@RequestMapping("/v1/_setup")
public class DatabaseSetupController {
    private final DatabaseSchemaService databaseSchemaService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;

    public DatabaseSetupController(
            DatabaseSchemaService databaseSchemaService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            JobInstanceDatabaseService jobInstanceDatabaseService) {
        this.databaseSchemaService = databaseSchemaService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
    }

    @PostMapping("database")
    public void ensureDatabaseSetup() throws SQLException {
        databaseSchemaService.ensureSchemaExistence(ETL_SCHEMA);
        // This is a simple ordered list for now since we only have 2 tables
        // with well-defined dependencies. If this grows more complex think about
        // a topological sort
        List<DatabaseService<?>> databaseServices = List.of(
                jobDefinitionDatabaseService,
                jobInstanceDatabaseService);
        for (DatabaseService<?> databaseService : databaseServices) {
            databaseService.ensureTableExistence();
        }
    }
}
