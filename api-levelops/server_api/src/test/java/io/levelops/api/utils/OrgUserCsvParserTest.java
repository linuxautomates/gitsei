package io.levelops.api.utils;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema.Field;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class OrgUserCsvParserTest {
    
    @Test
    public void test() throws IOException, SQLException {
        var tenant = "test";
        var integration1 = "My Integration";
        var integration2 = "Not my integration";
        var integrationService = Mockito.mock(IntegrationService.class);

        when(integrationService.listByFilter(eq(tenant), eq(integration1.toLowerCase()), any(), any(), any(), any(), eq(0), eq(1))).thenReturn(DbListResponse.of(List.of(Integration.builder().id("1").build()), 1));
        when(integrationService.listByFilter(eq(tenant), eq(integration2.toLowerCase()), any(), any(), any(), any(), eq(0), eq(1))).thenReturn(DbListResponse.of(List.of(Integration.builder().id("2").build()), 1));

        var parser = new OrgUserCsvParser(integrationService);

        var schema = OrgUserSchema.builder().build();
        var source = ResourceUtils.getResourceAsStream("utils/org_users_import.csv");
        var userStream = parser.map(tenant, source, schema);
        // userStream.forEach(user -> System.out.println("User: " + user));

        var users = userStream.collect(Collectors.toSet());
        Assertions.assertThat(users).hasSize(3);
        Assertions.assertThat(users).containsExactlyInAnyOrder(DBOrgUser.builder()
                .fullName("Test one")
                .email("test1@test.com")
                .ids(Set.of(LoginId.builder()
                        .cloudId("theTest1")
                        .username("theTest1")
                        .integrationId(0)
                        .build(),
                    LoginId.builder()
                        .cloudId("the_test1")
                        .username("the_test1")
                        .integrationId(0)
                        .build()
                    ))
                .customFields(Map.of())
                .build(),
            DBOrgUser.builder()
                .fullName("Test two")
                .email("test2@test.com")
                .ids(Set.of(LoginId.builder()
                        .cloudId("theTest2")
                        .username("theTest2")
                        .integrationId(0)
                        .build(),
                    LoginId.builder()
                        .cloudId("the_test2")
                        .username("the_test2")
                        .integrationId(0)
                        .build()
                    ))
                .customFields(Map.of())
                .build(),
            DBOrgUser.builder()
                .fullName("Test three")
                .email("test3@test.com")
                .ids(Set.of(LoginId.builder()
                        .cloudId("theTest3")
                        .username("theTest3")
                        .integrationId(0)
                        .build(),
                    LoginId.builder()
                        .cloudId("the_test3")
                        .username("the_test3")
                        .integrationId(0)
                        .build()
                    ))
                .customFields(Map.of())
                .build());

        var schema2 = OrgUserSchema.builder().fields(Set.of(
            Field.builder()
                .key("custom_field1")
                .type(Field.FieldType.STRING)
                .build(),
            Field.builder()
                .key("custom_field2")
                .type(Field.FieldType.STRING)
                .build(),
            Field.builder()
                .key("city")
                .type(Field.FieldType.STRING)
                .build())).build();
        var source2 = ResourceUtils.getResourceAsStream("utils/org_users_import.csv");
        var userStream2 = parser.map(tenant, source2, schema2);
        // userStream.forEach(user -> System.out.println("User: " + user));

        var users2 = userStream2.collect(Collectors.toSet());
        Assertions.assertThat(users2).hasSize(3);
        Assertions.assertThat(users2).containsExactlyInAnyOrder(DBOrgUser.builder()
                .fullName("Test one")
                .email("test1@test.com")
                .ids(Set.of(LoginId.builder()
                        .cloudId("theTest1")
                        .username("theTest1")
                        .integrationId(0)
                        .build(),
                    LoginId.builder()
                        .cloudId("the_test1")
                        .username("the_test1")
                        .integrationId(0)
                        .build()
                    ))
                .customFields(Map.of("custom_field1", "myTest1", "custom_field2", "ok", "city", "SF"))
                .build(),
            DBOrgUser.builder()
                .fullName("Test two")
                .email("test2@test.com")
                .ids(Set.of(LoginId.builder()
                        .cloudId("theTest2")
                        .username("theTest2")
                        .integrationId(0)
                        .build(),
                    LoginId.builder()
                        .cloudId("the_test2")
                        .username("the_test2")
                        .integrationId(0)
                        .build()
                    ))
                .customFields(Map.of("custom_field1", "myTest2", "custom_field2", "no", "city", "Chicago"))
                .build(),
            DBOrgUser.builder()
                .fullName("Test three")
                .email("test3@test.com")
                .ids(Set.of(LoginId.builder()
                        .cloudId("theTest3")
                        .username("theTest3")
                        .integrationId(0)
                        .build(),
                    LoginId.builder()
                        .cloudId("the_test3")
                        .username("the_test3")
                        .integrationId(0)
                        .build()
                    ))
                .customFields(Map.of("custom_field1", "myTest3", "custom_field2", "ok", "city", "Sunnyvale"))
                .build());
    }
}
