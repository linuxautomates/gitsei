package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.utils.ResourceUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import javax.sql.DataSource;

@Log4j2
@Service
public class BootstrapService {

    private static final String DEFAULT_SCM_VELOCITY_CONFIG_ID = "DEFAULT_SCM_VELOCITY_CONFIG_ID";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;
    private final TenantConfigService tenantConfigService;
    private final VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    private static final List<String> customerSuccessEmail = List.of("customersuccess@levelops.io", "customersuccess@propelo.ai", "sei-cs@harness.io");

    @Autowired
    public BootstrapService(DataSource dataSource,
                            ObjectMapper objectMapper,
                            TenantConfigService tenantConfigService,
                            VelocityConfigsDatabaseService velocityConfigsDatabaseService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
        this.tenantConfigService = tenantConfigService;
        this.velocityConfigsDatabaseService = velocityConfigsDatabaseService;
    }

    /**
     * DB initialization for elements that shoudl be inserted only at tenant creation meaning, that are not idempotent.
     */
    public void bootstrapTenant(final String tenant, final String defaultUserName, final String defaultUserLastName, final String defaultUserEmail) {
        String newUserStatement = "";
        // default user is the first user in the system other than customer support. customer support comes by default.
        if (StringUtils.isNotBlank(defaultUserName)
            && StringUtils.isNotBlank(defaultUserLastName)
            && StringUtils.isNotBlank(defaultUserEmail)
            && !customerSuccessEmail.contains(defaultUserEmail)) {
            String scopes = "'''{}'''";

            newUserStatement = "INSERT INTO {0}.users(firstname,lastname,email,usertype,passwordauthenabled,samlauthenabled, bcryptpassword, passwordreset, scopes) "
                    + "VALUES(''" + defaultUserName + "'',''" + defaultUserLastName + "'',''" + defaultUserEmail + "'', ''ADMIN'',true,false, decode(''013d'',''hex''), '''{}''', "+scopes+" );";
        }
        List<String> bootStrap = List.of(
            "INSERT INTO {0}.ticket_templates(name,enabled) VALUES(''Default Ticket Template'', true)",

            "INSERT INTO {0}.tenantconfigs(name,value)\n"
            + "VALUES\n"
            + "   (''DEFAULT_TICKET_TEMPLATE'', (SELECT id FROM {0}.ticket_templates WHERE name = ''Default Ticket Template''))\n"
            + "ON CONFLICT(name) DO NOTHING;",

            "INSERT INTO {0}.users(firstname,lastname,email,usertype,passwordauthenabled,samlauthenabled, bcryptpassword, passwordreset, scopes) "
            + "VALUES(''Customer'',''Success'',''sei-cs@harness.io'', ''ADMIN'',true,false, decode(''013d'',''hex''), '''{}''', '''{\"dev_productivity_write\": null}''');",

            newUserStatement);

        bootStrap.stream()
            .filter(StringUtils::isNotBlank)
            .peek(statement -> log.info("statement: " + statement))
            .map(statement -> MessageFormat.format(statement, tenant))
            .forEach(statement -> this.template.getJdbcTemplate().execute(statement));

        populateDefaultVelocityConfig(tenant);
    }

    private void populateDefaultVelocityConfig(String tenant) {
        try {
            String resourceString = ResourceUtils.getResourceAsString("db/default_data/velocity/default_scm_velocity_config.json", BootstrapService.class.getClassLoader());
            var velocityConfig = objectMapper.readValue(resourceString, VelocityConfig.class);
            String id = velocityConfigsDatabaseService.insert(tenant, velocityConfig);
            if (StringUtils.isEmpty(id)) {
                throw new IOException("Could not insert velocity config");
            }
            tenantConfigService.insert(tenant, TenantConfig.builder()
                    .name(DEFAULT_SCM_VELOCITY_CONFIG_ID)
                    .value(id)
                    .build());
            log.info("Inserted default scm velocity config for tenant={}: id={}", tenant, id);
        } catch (IOException | SQLException e) {
            log.warn("Failed to populate default velocity config", e);
        }
    }
    
}