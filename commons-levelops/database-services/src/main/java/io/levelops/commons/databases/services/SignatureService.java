package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.Signature;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.functional.IterableUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// TODO remove this - also this code wont work
@Deprecated
@Log4j2
public class SignatureService extends DatabaseService<Signature> {

    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;

    public SignatureService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.mapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * NOTE: this does not return id unless insert was successful.
     */
    @Override
    public String insert(String company, Signature signature) throws SQLException {
        String sql = "INSERT INTO :company.signatures(" +
                "    type," +
                "    output_type, " +
                "    description," +
                "    product_ids," +
                "    labels," +
                "    config) " +
                "VALUES (" +
                "    :type," +
                "    :output_type::output_type_t," +
                "    :product_ids::INTEGER[]," +
                "    to_json(:labels::json)," +
                "    to_json(:config::json))" +
                "RETURNING id";
        Map<String, ?> values = Map.of();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(values), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return (String) keyHolder.getKeys().get("id");
    }

    @Override
    public Boolean update(String company, Signature signature) throws SQLException {

        List<String> updates = Lists.newArrayList();
        Map<String, Object> values = Maps.newHashMap();

        try {
            if (signature.getOutputType() != null) {
                updates.add("output_type = :output_type::output_type_t");
                values.put("output_type", signature.getOutputType().toString());
            }
            if (signature.getDescription() != null) {
                updates.add("description = :description");
                values.put("description", signature.getDescription());
            }
            if (signature.getProductIds() != null) {
                updates.add("product_ids = :product_ids::integer[]");
                values.put("product_ids", DatabaseUtils.toSqlArray(signature.getProductIds()));
            }
            if (signature.getConfig() != null) {
                updates.add("config = to_json(:config::json) ");
                values.put("config", mapper.writeValueAsString(signature.getConfig()));
            }
            if (signature.getLabels() != null) {
                updates.add("labels = to_json(:labels::json)");
                values.put("labels", mapper.writeValueAsString(signature.getLabels()));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize signature {}", signature, e);
            return false;
        }

        if (updates.isEmpty()) {
            return true;
        }

        String sql = "UPDATE :company.signature SET "
                + String.join(", ", updates)
                + " WHERE id = :id";
        values.put("id", signature.getId());

        return template.execute(sql, values, ps -> true);

    }

    @Override
    public Optional<Signature> get(String company, String signatureId) throws SQLException {
        String SQL = "SELECT * FROM :company.signatures " +
                " WHERE id = :id " +
                " LIMIT 1 ";

        List<Signature> signatures = template.queryForList(SQL, Map.of(
                "company", company,
                "id", signatureId
        ), Signature.class);
        return IterableUtils.getFirst(signatures);
    }

    @Override
    public DbListResponse<Signature> list(String company, Integer pageNumber,
                                          Integer pageSize) throws SQLException {
        String sql = "SELECT * FROM :company.signatures " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<Signature> signatures = template.queryForList(sql, Map.of(
                "company", company,
                "skip", MoreObjects.firstNonNull(pageNumber, 0),
                "limit", MoreObjects.firstNonNull(pageNumber, 50)
        ), Signature.class);
        return DbListResponse.of(signatures, null);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM :company.signatures WHERE id = :id";

        return template.execute(sql, Map.of(
                "company", company,
                "id", id
        ), ps -> true);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        DatabaseUtils.createEnumType(template.getJdbcTemplate(),
                "output_type_t", Signature.OutputType.class);

        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS :company.signatures (" +
                        "        id                UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "        type              VARCHAR(255) NOT NULL," +
                        "        output_type       output_type_t NOT NULL DEFAULT 'items'," +
                        "        description       TEXT," +
                        "        product_ids       INTEGER[] NOT NULL DEFAULT '{}'::INTEGER[]," +
                        "        labels            JSONB," +
                        "        config            JSONB," +
                        "        created_at        BIGINT NOT NULL DEFAULT extract(epoch from now())," +
                        ")",
                "CREATE INDEX IF NOT EXISTS type_idx on :company.signature (type)",
                "CREATE INDEX IF NOT EXISTS product_ids_idx on :company.signature (product_ids)"
        );

        Map<String, Object> params = Map.of("company", company);
        sqlList.forEach(sql -> template.execute(sql, params, ps -> true));

        return true;
    }
}
