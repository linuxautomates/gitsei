package io.levelops.commons.databases.utils;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper.AggTimeQuery;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class AggTimeQueryHelperTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";

    DataSource dataSource;
    NamedParameterJdbcTemplate template;

    @Before
    public void setUp() throws Exception {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Test
    public void testBiweekly() {
        AggTimeQueryHelper.Options options = AggTimeQueryHelper.Options.builder()
                .columnName("ts")
                .across("ts")
                .interval("biweekly")
                .isBigInt(true)
                .prefixWithComma(false)
                .build();

        AggTimeQuery query = AggTimeQueryHelper.getAggTimeQuery(options);
        verifyBiweekly(query.getHelperColumn());

        AggTimeQuery query2 = AggTimeQueryHelper.getAggTimeQueryForList(options);
        verifyBiweekly(query2.getHelperColumn());
    }

    private void verifyBiweekly(String sql) {

        DefaultObjectMapper.prettyPrint(sql);

        int offset = TimeZone.getDefault().getRawOffset()/(1000*60*60);
        String offsetStr = (offset > 0 ? "+" : "-") + String.format("%02d", Math.abs(offset));
        String data = "(select unnest(array[" + 
            Instant.parse("2022-01-01T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-02T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-03T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-04T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-05T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-17T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-12-18T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-12-31T00:00:00" + offsetStr + ":00" ).getEpochSecond() +
            // 1640995200, 1641108000, 1641168000, 1641254400, 1641340800, 1642377600, 1671321600, 1672444800
            "]) as ts) as data";
        Map<String, String> expectedInputOutput = Map.of(
                // "2021-12-31", "2021-12-20",
                "2022-01-01", "2021-12-20",
                "2022-01-02", "2021-12-20",
                "2022-01-03", "2022-01-03",
                "2022-01-04", "2022-01-03",
                "2022-01-05", "2022-01-03",
                "2022-01-17", "2022-01-17",
                "2022-12-18", "2022-12-05",
                "2022-12-31", "2022-12-19"
        );

        SqlRowSet rowSet = template.getJdbcTemplate().queryForRowSet("select ts, to_timestamp(ts) as ts_date, " + sql + " from " + data);

        int count = 0;
        while (rowSet.next()) {
            Long ts = rowSet.getLong("ts");
            Date tsDate = rowSet.getDate("ts_date");
            Date out = rowSet.getDate("ts_interval");
            count++;
            System.out.println(ts + " " + tsDate + " -> " + out);
            assertThat(out.toString()).isEqualTo(expectedInputOutput.get(tsDate.toString()));
        }
        assertThat(count).isEqualTo(8);
    }

    @Test
    public void testAggWeekly() {
        AggTimeQuery query = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                .columnName("ts")
                .across("ts")
                .interval("week")
                .isBigInt(true)
                .prefixWithComma(false)
                .build());

        DefaultObjectMapper.prettyPrint(query);

        int offset = TimeZone.getDefault().getRawOffset()/(1000*60*60);
        String offsetStr = (offset > 0 ? "+" : "-") + String.format("%02d", Math.abs(offset));
        String data = "(select unnest(array[" + 
            Instant.parse("2022-01-01T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-02T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-03T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-04T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-05T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-01-17T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-12-18T00:00:00" + offsetStr + ":00" ).getEpochSecond() + "," +
            Instant.parse("2022-12-31T00:00:00" + offsetStr + ":00" ).getEpochSecond() +
            // ", 1641081600, 1641168000, 1641254400, 1641340800, 1642377600, 1671321600, 1672444800" + 
            "]) as ts) as data";
        Map<String, String> expectedInputOutput = Map.of(
                "2022-01-01", "52-2021",
                "2022-01-02", "52-2021",
                "2022-01-03", "1-2022",
                "2022-01-04", "1-2022",
                "2022-01-05", "1-2022",
                "2022-01-17", "3-2022",
                "2022-12-18", "50-2022",
                "2022-12-31", "52-2022"
        );

        SqlRowSet rowSet = template.getJdbcTemplate().queryForRowSet("" +
                "select ts, ts_date, " + query.getSelect() + " from " +
                "(select ts, to_timestamp(ts) as ts_date, " + query.getHelperColumn()
                + " from " + data +
                ") final");

        int count = 0;
        while (rowSet.next()) {
            Long ts = rowSet.getLong("ts");
            Date tsDate = rowSet.getDate("ts_date");
            String out = rowSet.getString("interval");
            count++;
            System.out.println(ts + " " + tsDate + " -> " + out);
            assertThat(out).isEqualTo(expectedInputOutput.get(tsDate.toString()));
        }
        assertThat(count).isEqualTo(8);
    }


}