package io.levelops.internal_api.controllers;

import com.google.api.client.util.Sets;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.BootstrapService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1")
public class DatabaseSetupController {

    private final DatabaseSchemaService databaseSchemaService;
    private final TenantService tenantService;
    private final List<DatabaseService<?>> databaseServices;
    private final BootstrapService bootstrapService;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public DatabaseSetupController(final DatabaseSchemaService databaseSchemaService,
                                   final TenantService tenantService,
                                   final List<DatabaseService<?>> databaseServices,
                                   final BootstrapService bootstrapService,
                                   final NamedParameterJdbcTemplate template) {
        this.databaseSchemaService = databaseSchemaService;
        this.tenantService = tenantService;
        this.databaseServices = databaseServices;
        this.bootstrapService = bootstrapService;
        this.template = template;
    }

    /**
     * Returns tables' dependency graph as map of edges from a set of requirements to a set of dependencies.
     * E.g.
     * Map.of (
     * { } -> { tables with no requirements, ... },
     * {Required-A, Required-B} -> { dependency-1, dependency-2, ... }
     * )
     */
    protected static Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> buildDependencyGraph(final List<DatabaseService<?>> databaseServices) {
        Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> dependencyGraph = Maps.newHashMap();
        databaseServices.stream()
                .filter(DatabaseService::isTenantSpecific)
                .forEach(dbService -> {
                    Set<Class<? extends DatabaseService<?>>> references = dbService.getReferences();
                    Set<DatabaseService<?>> services = dependencyGraph.getOrDefault(references, Sets.newHashSet());
                    services.add(dbService);
                    dependencyGraph.put(references, services);
                });
        return dependencyGraph;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/_ensure_internal_schema", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> ensureInternalDatabaseSetup() {
        return SpringUtils.deferResponse(() -> {
            for (DatabaseService<?> databaseService : databaseServices) {
                if (databaseService.getSchemaType() == DatabaseService.SchemaType.LEVELOPS_INVENTORY_SCHEMA) {
                    databaseService.ensureTableExistence(null);
                }
            }
            return ResponseEntity.ok().build();
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/_ensure_tenant_schema", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> ensureSchema(
            @RequestParam("company") String company,
            @RequestParam("company_name") String companyName,
            @RequestParam(value = "default_user_name", required = false) String defaultUserName,
            @RequestParam(value = "default_user_lastname", required = false) String defaultUserLastName,
            @RequestParam(value = "default_user_email", required = false) String defaultUserEmail,
            @RequestParam(value = "create_only", defaultValue = "false") Boolean createOnly) {

        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(company) || !company.matches("[a-z0-9]+")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid company id provided. Only lower-case letters and numbers allowed.");
            }

            var newTenant = createTenant(company, companyName, createOnly);
            databaseSchemaService.ensureSchemaExistence(company);

            // build dependency graph
            Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> dependencyGraph = buildDependencyGraph(databaseServices);

            // keeps track of tables that have been visited
            Set<Class<? extends DatabaseService<?>>> visited = Sets.newHashSet();

            // visit tables with no requirements first
            visitTables(company, dependencyGraph, Collections.emptySet(), visited);

            // traverse dependency graph
            int prevVisitedSize = visited.size();
            while (!dependencyGraph.isEmpty()) {
                // for each set of requirements;
                // if the whole set has been visited, then visit the tables that required that set
                Set<Set<Class<? extends DatabaseService<?>>>> keySet = Sets.newHashSet();
                keySet.addAll(dependencyGraph.keySet());
                for (Set<Class<? extends DatabaseService<?>>> required : keySet) {
                    if (visited.containsAll(required)) {
                        visitTables(company, dependencyGraph, required, visited);
                    }
                }
                if (prevVisitedSize == visited.size()) {
                    throw newDependencyGraphException(dependencyGraph, visited);
                }
                prevVisitedSize = visited.size();
                log.debug("Visited tables count: " + prevVisitedSize);
            }
            log.info("Done. (visited {} tables)", prevVisitedSize);

            log.debug("Granting permissions for any new tables...");
            ensureSupportPermissions(company);

            if (newTenant && !createOnly) {
                bootstrapService.bootstrapTenant(company, defaultUserName, defaultUserLastName, defaultUserEmail);
            }
            return ResponseEntity.ok().build();
        });
    }

    /**
     * Given a dependency graph and a set of requirements, visits all tables that are dependent on those requirements;
     * then removes the requirements from the graph.
     * (adds them to the set of visited tables and runs 'ensure existence of tables')
     */
    @SuppressWarnings({"unchecked"})
    private void visitTables(String company, Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> dependencyGraph,
                             Set<Class<? extends DatabaseService<?>>> required,
                             Set<Class<? extends DatabaseService<?>>> visited) throws SQLException {
        log.info("Requirements: {}", required.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toList()));
        Set<DatabaseService<?>> toVisit = dependencyGraph.get(required);
        ensureExistenceOfTables(company, toVisit);
        //noinspection unchecked
        toVisit.forEach(srv -> visited.add((Class<? extends DatabaseService<?>>) srv.getClass()));
        dependencyGraph.remove(required);
    }

    private Exception newDependencyGraphException(Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> dependencyGraph,
                                                  Set<Class<? extends DatabaseService<?>>> visited) {
        Map<List<String>, List<String>> graphString = Maps.newHashMap();
        dependencyGraph.forEach((key, value) -> graphString.put(
                key.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.toList()),
                value.stream()
                        .map(srv -> srv.getClass().getSimpleName())
                        .collect(Collectors.toList())));

        String visitedString = visited.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toList()).toString();
        return new Exception("Failed to visit dependency graph of tables." +
                " Visited= " + visitedString +
                " Remaining graph=" + graphString.toString());
    }

    List<String> supportPermissions = List.of(
        "DO $$\n" 
        + "BEGIN\n"
        + "   IF NOT EXISTS (SELECT * FROM pg_catalog.pg_roles WHERE  rolname = ''levelops_readonly'') THEN\n"
        + "      CREATE ROLE levelops_readonly WITH\n"
        + "        NOLOGIN\n"
        + "        NOSUPERUSER\n"
        + "        NOCREATEDB\n"
        + "        NOCREATEROLE\n"
        + "        INHERIT\n"
        + "        NOREPLICATION\n"
        + "        CONNECTION LIMIT 3;\n"
        + "   END IF;\n"
        + "END$$;\n",

        "GRANT SELECT ON ALL TABLES IN SCHEMA {0} TO GROUP levelops_readonly;",
        "GRANT SELECT ON ALL SEQUENCES IN SCHEMA {0} TO GROUP levelops_readonly;",
        "GRANT USAGE ON SCHEMA {0} TO GROUP levelops_readonly;",

        "DO $$\n"
        + "BEGIN\n"
        + "   IF NOT EXISTS (SELECT * FROM pg_catalog.pg_roles WHERE  rolname = ''levelops_write'') THEN\n"
        + "      CREATE ROLE levelops_write WITH\n"
        + "        NOLOGIN\n"
        + "        NOSUPERUSER\n"
        + "        NOCREATEDB\n"
        + "        NOCREATEROLE\n"
        + "        INHERIT\n"
        + "        NOREPLICATION\n"
        + "        CONNECTION LIMIT 2;\n"
        + "   END IF;\n"
        + "END$$;\n",

        "GRANT ALL ON ALL TABLES IN SCHEMA {0} TO GROUP levelops_write';",
        "GRANT ALL ON ALL SEQUENCES IN SCHEMA {0} TO GROUP levelops_write';",
        "GRANT USAGE ON SCHEMA {0} TO GROUP levelops_write';"
        );

    private void ensureSupportPermissions(final String company) {
        supportPermissions.stream()
            .map(statement -> MessageFormat.format(statement, company))
            .forEach(sql -> template.getJdbcOperations().execute(sql));
    }

    private void ensureExistenceOfTables(String company, Iterable<DatabaseService<?>> dbServices) throws SQLException {
        for (DatabaseService<?> databaseService : dbServices) {
            if (databaseService.isTenantSpecific()) {
                log.info("Ensuring existence of table=" + databaseService.getClass().getSimpleName());
                databaseService.ensureTableExistence(company);
            }
        }
    }

    private boolean createTenant(String company, String companyName, boolean createOnly) throws SQLException {
        Optional<Tenant> tenant = tenantService.get(null, company);
        if (tenant.isPresent() && createOnly) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Company already exists and create_only is true.");
        }
        if (tenant.isPresent()) {
            return false;
        }
        //tenant is not present so we are creating them.
        tenantService.insert(null, Tenant.builder()
                .id(company)
                .tenantName(companyName)
                .build());
        return true;
    }

}
