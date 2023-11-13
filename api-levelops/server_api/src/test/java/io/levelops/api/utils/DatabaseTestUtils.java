package io.levelops.api.utils;

import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DatabaseTestUtils {
    public static final String arrayUniq = "CREATE OR REPLACE FUNCTION anyarray_uniq(with_array anyarray)\n" +
            "\tRETURNS anyarray AS\n" +
            "$BODY$\n" +
            "\tDECLARE\n" +
            "\t\t-- The variable used to track iteration over \"with_array\".\n" +
            "\t\tloop_offset integer;\n" +
            "\n" +
            "\t\t-- The array to be returned by this function.\n" +
            "\t\treturn_array with_array%TYPE := '{}';\n" +
            "\tBEGIN\n" +
            "\t\tIF with_array IS NULL THEN\n" +
            "\t\t\treturn NULL;\n" +
            "\t\tEND IF;\n" +
            "\t\t\n" +
            "\t\tIF with_array = '{}' THEN\n" +
            "\t\t    return return_array;\n" +
            "\t\tEND IF;\n" +
            "\n" +
            "\t\t-- Iterate over each element in \"concat_array\".\n" +
            "\t\tFOR loop_offset IN ARRAY_LOWER(with_array, 1)..ARRAY_UPPER(with_array, 1) LOOP\n" +
            "\t\t\tIF with_array[loop_offset] IS NULL THEN\n" +
            "\t\t\t\tIF NOT EXISTS(\n" +
            "\t\t\t\t\tSELECT 1 \n" +
            "\t\t\t\t\tFROM UNNEST(return_array) AS s(a)\n" +
            "\t\t\t\t\tWHERE a IS NULL\n" +
            "\t\t\t\t) THEN\n" +
            "\t\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\t\tEND IF;\n" +
            "\t\t\t-- When an array contains a NULL value, ANY() returns NULL instead of FALSE...\n" +
            "\t\t\tELSEIF NOT(with_array[loop_offset] = ANY(return_array)) OR NOT(NULL IS DISTINCT FROM (with_array[loop_offset] = ANY(return_array))) THEN\n" +
            "\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\tEND IF;\n" +
            "\t\tEND LOOP;\n" +
            "\n" +
            "\tRETURN return_array;\n" +
            " END;\n" +
            "$BODY$ LANGUAGE plpgsql;";
    public static DataSource setUpDataSource(SingleInstancePostgresRule pg, String company) throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS " + company + " CASCADE; ",
                "CREATE SCHEMA " + company + " ; "
        ).forEach(template::execute);
        dataSource.getConnection().prepareStatement(arrayUniq).execute();
        return dataSource;
    }
}
