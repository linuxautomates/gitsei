package io.levelops.controlplane.controllers;

import io.levelops.controlplane.database.DatabaseSchemaService;
import io.levelops.controlplane.database.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

import static io.levelops.controlplane.database.ControlPlaneDatabaseConstants.CONTROL_PLANE_SCHEMA;

@RestController
@RequestMapping("/v1/_setup")
public class DatabaseSetupController {

    private final DatabaseSchemaService databaseSchemaService;
    private final List<DatabaseService> databaseServices;

    @Autowired
    public DatabaseSetupController(
            DatabaseSchemaService databaseSchemaService,
            List<DatabaseService> databaseServices) {
        this.databaseSchemaService = databaseSchemaService;
        this.databaseServices = databaseServices;
    }

    @PostMapping("database")
    public void ensureDatabaseSetup() throws SQLException {
        databaseSchemaService.ensureSchemaExistence(CONTROL_PLANE_SCHEMA);
        for (DatabaseService databaseService : databaseServices) {
            databaseService.ensureTableExistence();
        }
    }

}
