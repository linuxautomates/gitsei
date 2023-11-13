package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final Map<String, List<String>> DEV_PROD_SCOPES = new HashMap<>();

    static {
        DEV_PROD_SCOPES.put("dev_productivity_write", List.of());
    }

    private static final String COMPANY = "test";
    private static final String COMPANY2 = "foo";
    private static final String LEVELOPS_INVENTORY_SCHEMA = "_levelops";
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private UserService userService;
    private TenantService tenantService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA " + COMPANY + ";"
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        userService.ensureTableExistence(COMPANY);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(LEVELOPS_INVENTORY_SCHEMA);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(COMPANY2);

        tenantService = new TenantService(dataSource);
        tenantService.ensureTableExistence(LEVELOPS_INVENTORY_SCHEMA);
        tenantService.insert(LEVELOPS_INVENTORY_SCHEMA, Tenant.builder().id("test").tenantName("test").build());
        tenantService.insert(LEVELOPS_INVENTORY_SCHEMA, Tenant.builder().id("foo").tenantName("foo").build());

        userService = new UserService(dataSource, DefaultObjectMapper.get());
        userService.ensureTableExistence(COMPANY2);

    }

    private String insert(String email) throws SQLException {
        return insert(email, false, null, null);
    }

    private String insert(String email, Boolean mfa, Instant enrollment, Instant mfaReset) throws SQLException {
        return userService.insert(COMPANY, User.builder()
                .firstName("f")
                .lastName("l")
                .bcryptPassword("p")
                .email(email)
                .userType(RoleType.ADMIN)
                .passwordAuthEnabled(true)
                .samlAuthEnabled(false)
                .mfaEnabled(mfa)
                .scopes(Map.of("dev_productivity_write", List.of()))
                .mfaEnrollmentEndAt(enrollment)
                .mfaResetAt(mfaReset)
                .managedOURefIds(List.of(1,2))
                .build());
    }

    @Test
    public void testInsert() throws SQLException {
        String user1Id = insert("a");
        Optional<User> user1ReadOpt = userService.get(COMPANY, user1Id);
        assertThat(user1ReadOpt).isPresent();
        assertThat(user1ReadOpt.get().getEmail()).isEqualTo("a");
        assertThat(user1ReadOpt.get().getManagedOURefIds()).isEqualTo(List.of(1,2));
        User user1Updated = user1ReadOpt.get().toBuilder().id(user1Id).scopes(DEV_PROD_SCOPES).build();
        Assert.assertTrue(userService.updateScopes(COMPANY, user1Id, DEV_PROD_SCOPES));
        user1Updated = user1Updated.toBuilder().managedOURefIds(List.of(2,3)).build();
        Assert.assertTrue(userService.updateUserManagedOus(COMPANY, user1Id, List.of(2,3)));
        //when updated with same ou_ids, update shouldn't happen
        user1Updated = user1Updated.toBuilder().managedOURefIds(List.of(2,3)).build();
        Assert.assertTrue(userService.updateUserManagedOus(COMPANY, user1Id, List.of(2,3)));
        user1ReadOpt = userService.get(COMPANY, user1Id);
        User user1Read = user1ReadOpt.get();
        user1Updated = user1Updated.toBuilder().updatedAt(user1Read.getUpdatedAt()).build();
        Assert.assertEquals(user1Updated, user1Read);
        user1Updated = user1Updated.toBuilder().managedOURefIds(List.of(4,5)).build();
        userService.update(COMPANY, user1Updated, true);
        user1Read = userService.get(COMPANY, user1Id).get();
        assertThat(user1Read.getManagedOURefIds()).isEqualTo(List.of(2,3));

        String id2 = insert("b", true, null, null);
        Optional<User> get2 = userService.get(COMPANY, id2);
        assertThat(get2).isPresent();
        assertThat(get2.get().getEmail()).isEqualTo("b");
        assertThat(get2.get().getMfaEnabled()).isEqualTo(true);
        assertThat(get2.get().getMfaEnrollmentEndAt()).isNull();
        assertThat(get2.get().getMfaResetAt()).isNull();

        String id3 = insert("c", true, Instant.parse("2021-06-24T08:00:00+08:00"), null);
        Optional<User> get3 = userService.get(COMPANY, id3);
        assertThat(get3).isPresent();
        assertThat(get3.get().getEmail()).isEqualTo("c");
        assertThat(get3.get().getMfaEnabled()).isEqualTo(true);
        assertThat(get3.get().getMfaEnrollmentEndAt()).isEqualTo(Instant.parse("2021-06-24T08:00:00+08:00"));
        assertThat(get3.get().getMfaResetAt()).isNull();

        String id4 = insert("d", true, Instant.parse("2021-06-24T08:00:00+08:00"), Instant.parse("2021-06-23T08:00:00+08:00"));
        Optional<User> get4 = userService.get(COMPANY, id4);
        assertThat(get4).isPresent();
        assertThat(get4.get().getEmail()).isEqualTo("d");
        assertThat(get4.get().getMfaEnabled()).isEqualTo(true);
        assertThat(get4.get().getMfaEnrollmentEndAt()).isEqualTo(Instant.parse("2021-06-24T08:00:00+08:00"));
        assertThat(get4.get().getMfaResetAt()).isNull();

        Boolean updated = userService.update(COMPANY, get4.get().toBuilder().mfaResetAt(Instant.parse("2021-06-23T08:00:00+08:00")).build());
        assertThat(updated).isTrue();
        Optional<User> get5 = userService.get(COMPANY, id4);
        assertThat(get5).isPresent();
        assertThat(get5.get().getEmail()).isEqualTo("d");
        assertThat(get5.get().getMfaEnabled()).isEqualTo(true);
        assertThat(get5.get().getMfaEnrollmentEndAt()).isEqualTo(Instant.parse("2021-06-24T08:00:00+08:00"));
        // assertThat(get5.get().getMfaResetAt()).isNull();
        assertThat(get5.get().getMfaResetAt()).isEqualTo(Instant.parse("2021-06-23T08:00:00+08:00"));

        String id6 = insert("sei-cs@harness.io", true, Instant.parse("2021-06-24T08:00:00+08:00"), Instant.parse("2021-06-23T08:00:00+08:00"));
        Optional<User> get6 = userService.get(COMPANY, id6);
        assertThat(get6).isPresent();
        assertThat(get6.get().getEmail()).isEqualTo("sei-cs@harness.io");
        assertThat(get4.get().getMfaEnabled()).isEqualTo(true);
        assertThat(get4.get().getMfaEnrollmentEndAt()).isEqualTo(Instant.parse("2021-06-24T08:00:00+08:00"));
        assertThat(get4.get().getMfaResetAt()).isNull();

        // Test inserting null mfa_enabled
        String id7 = insert("s@propelo.ai", null, Instant.parse("2021-06-24T08:00:00+08:00"), Instant.parse("2021-06-23T08:00:00+08:00"));
        Optional<User> get7 = userService.get(COMPANY, id7);
        assertThat(get7).isPresent();
        // Default is false at a DB level
        assertThat(get7.get().getMfaEnabled()).isFalse();

        Boolean updated7 = userService.update(COMPANY, get7.get().toBuilder().mfaEnabled(true).build());
        assertThat(updated7).isTrue();
        get7 = userService.get(COMPANY, id7);
        assertThat(get7).isPresent();
        assertThat(get7.get().getMfaEnabled()).isTrue();

        // Not updating mfa_present, this should stay true
        updated7 = userService.update(COMPANY, get7.get().toBuilder().lastName("B").build());
        assertThat(updated7).isTrue();
        get7 = userService.get(COMPANY, id7);
        assertThat(get7).isPresent();
        assertThat(get7.get().getMfaEnabled()).isTrue();
        assertThat(get7.get().getLastName()).isEqualTo("B");
    }

    @Test
    public void testFilter() throws SQLException {
        String id1 = insert("aaa");
        String id2 = insert("bbb");
        String id3 = insert("ccc");

        // -- ids
        DbListResponse<User> output = userService.listByFilters(COMPANY, List.of(), null, null, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1, id2, id3);

        output = userService.listByFilters(COMPANY, List.of(id1, id3), null, null, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1, id3);

        output = userService.listByFilters(COMPANY, List.of("999999"), null, null, null, null);
        assertThat(output.getRecords()).isEmpty();

        // -- prefix
        output = userService.listByFilters(COMPANY, null, "aa", null, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1);

        output = userService.listByFilters(COMPANY, null, "F", RoleType.ADMIN, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1, id2, id3);

        output = userService.listByFilters(COMPANY, null, "L", RoleType.ADMIN, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1, id2, id3);

        // -- role
        output = userService.listByFilters(COMPANY, null, null, RoleType.ADMIN, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1, id2, id3);

        output = userService.listByFilters(COMPANY, null, null, RoleType.LIMITED_USER, null, null);
        assertThat(output.getRecords()).isEmpty();

        // -- combination
        output = userService.listByFilters(COMPANY, List.of(id1, id3), "c", RoleType.ADMIN, null, null);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id3);

        //-- managed ous
        output = userService.listByFilters(COMPANY,null,null,null,null,null, List.of("1"),0,10);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1, id2, id3);

        // -- pagination
        output = userService.listByFilters(COMPANY, List.of(), null, null, null, 2);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id3, id2);

        output = userService.listByFilters(COMPANY, List.of(), null, null, 1, 2);
        assertThat(output.getRecords().stream().map(User::getId)).containsExactlyInAnyOrder(id1);

        Optional<User> userForLogin = userService.getForAuthOnly(COMPANY, "aaa", null);
        assertThat(userForLogin.get().getScopes().containsKey("dev_productivity_write"));

        userForLogin = userService.getForAuthOnly(COMPANY, "aaa", true);
        assertThat(userForLogin.get().getScopes().containsKey("dev_productivity_write"));

    }

    @Test
    public void test() throws SQLException {

        insert(COMPANY, "ashish@propelo.ai", false, null, null);
        insert(COMPANY2, "ashish@propelo.ai", true, null, null);
        insert(COMPANY, "ashish@levelops.io", false, null, null);
        insert(COMPANY2, "ashish@levelops.io", false, null, null);

        List<String> tenantList = List.of("test", "foo");

        List<User> user = userService.getUserDetailsAcrossTenants("ashish@propelo.ai", tenantList);
        assertThat(user.size()).isEqualTo(2);

        Boolean result = userService.isMultiTenantUser("ashish@propelo.ai", tenantList);
        assertThat(result).isTrue();

        result = userService.isMultiTenantUser("ashish@abc.com", tenantList);
        assertThat(result).isNull();

        result = userService.isValidCompanyUser(COMPANY, "ashish@propelo.ai");
        assertThat(result).isTrue();

        result = userService.isValidCompanyUser(COMPANY, "ashish@abc.com");
        assertThat(result).isFalse();

    }

    private String insert(String company, String email, Boolean mfa, Instant enrollment, Instant mfaReset) throws SQLException {
        return userService.insert(company, User.builder()
                .firstName("f")
                .lastName("l")
                .bcryptPassword("p")
                .email(email)
                .userType(RoleType.ADMIN)
                .passwordAuthEnabled(true)
                .samlAuthEnabled(false)
                .mfaEnabled(mfa)
                .scopes(Map.of("dev_productivity_write", List.of()))
                .mfaEnrollmentEndAt(enrollment)
                .mfaResetAt(mfaReset)
                .build());
    }
}