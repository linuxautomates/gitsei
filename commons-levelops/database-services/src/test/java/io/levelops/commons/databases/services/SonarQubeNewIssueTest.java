package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeIssue;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class SonarQubeNewIssueTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static SonarQubeIssueService sonarQubeIssueService;
    private static SonarQubeProjectService sonarQubeProjectService;

    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        sonarQubeIssueService = new SonarQubeIssueService(dataSource);
        sonarQubeProjectService = new SonarQubeProjectService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        sonarQubeIssueService.ensureTableExistence(company);
        sonarQubeProjectService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);

        integrationService.insert(company,
                Integration.builder()
                        .id("1")
                        .name("sonarqube")
                        .url("http://host:port")
                        .application("sonarqube")
                        .status("status")
                        .description("")
                        .build());

        List<DbSonarQubeIssue> Oldissues = List.of(
                DbSonarQubeIssue.builder().project("project1").key("key1").integrationId("1").ingestedAt(currentTime).build(),
                DbSonarQubeIssue.builder().project("project2").key("key1").integrationId("1").ingestedAt(currentTime).build(),
                DbSonarQubeIssue.builder().project("project3").key("key1").integrationId("1").ingestedAt(currentTime).build(),
                DbSonarQubeIssue.builder().project("project4").key("key1").integrationId("1").ingestedAt(currentTime).build()
        );

        Oldissues.forEach(issue -> {
            try {
                sonarQubeIssueService.insert(company, issue);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

    }

    @Test
    public void testNewIssuesIdentification() {
        List<DbSonarQubeIssue> issues = List.of(
                DbSonarQubeIssue.builder().project("project1").key("key1").integrationId("1").ingestedAt(currentTime).build(),
                DbSonarQubeIssue.builder().project("project2").key("key1").integrationId("1").ingestedAt(currentTime).build(),
                DbSonarQubeIssue.builder().project("project3").key("key1").integrationId("1").ingestedAt(currentTime).build(),
                DbSonarQubeIssue.builder().project("project5").key("key1").integrationId("1").ingestedAt(currentTime).build(), // new issue
                DbSonarQubeIssue.builder().project("project6").key("key1").integrationId("1").ingestedAt(currentTime).build()  // new issue
        );
        int count = (int) issues.stream()
                .map(issue -> sonarQubeIssueService.getId(company, issue.getKey(), issue.getProject(), issue.getIntegrationId()))
                .filter(Optional::isEmpty).count();
        Assert.assertEquals(count, 2);
    }

    private Timestamp convertJavaDateToSqlDate(java.util.Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}
