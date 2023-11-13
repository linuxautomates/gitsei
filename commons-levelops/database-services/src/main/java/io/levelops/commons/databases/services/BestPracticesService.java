package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.models.DbListResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BestPracticesService extends DatabaseService<BestPracticesItem> {

    protected final NamedParameterJdbcTemplate template;

    @Autowired
    public BestPracticesService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, BestPracticesItem kbItem) throws SQLException {

        String SQL = "INSERT INTO " + company + ".bpracticesitems(id,name,type,value,metadata) " +
                "VALUES(?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            var id = kbItem.getId() != null ? kbItem.getId() : UUID.randomUUID();
            pstmt.setObject(1, id);
            pstmt.setString(2, kbItem.getName());
            pstmt.setString(3, String.valueOf(kbItem.getType()));
            pstmt.setString(4, kbItem.getValue());
            pstmt.setString(5, kbItem.getMetadata());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                return id.toString();
            }
        }
        return null;
    }

    @Override
    public Boolean update(String company, BestPracticesItem kbItem) throws SQLException {
        String SQL = "UPDATE " + company + ".bpracticesitems SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(kbItem.getName())) {
            updates = "name = ?";
            values.add(kbItem.getName());
        }
        if (kbItem.getType() != null) {
            updates = StringUtils.isEmpty(updates) ? "type = ?" : updates + ", type = ?";
            values.add(kbItem.getType().toString());
        }
        if (StringUtils.isNotEmpty(kbItem.getValue())) {
            updates = StringUtils.isEmpty(updates) ? "value = ?" : updates + ", value = ?";
            values.add(kbItem.getValue());
        }
        if (StringUtils.isNotEmpty(kbItem.getMetadata())) {
            updates = StringUtils.isEmpty(updates) ? "metadata = ?" : updates + ", metadata = ?";
            values.add(kbItem.getMetadata());
        }

        updates += StringUtils.isEmpty(updates) ? "updatedat = ?" : ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());

        SQL = SQL + updates + condition;
        //no updates
        if (values.size() == 0) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
            }
            pstmt.setObject(values.size() + 1, kbItem.getId());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<BestPracticesItem> get(String company, String bpItemId) throws SQLException {
        String SQL = "SELECT bp.id,"
                + " bp.name,"
                + " bp.type,"
                + " bp.value,"
                + " bp.metadata,"
                + " bp.updatedat,"
                + " bp.createdat,"
                + " array_remove(array_agg(t.tagid), NULL)::text[] as tags"
                + " FROM "
                + company + ".bpracticesitems as bp"
                + " LEFT OUTER JOIN ("
                + " SELECT ti.tagid, ti.itemid::uuid"
                + " FROM "
                + company + ".tagitems as ti"
                + " WHERE ti.itemtype = '" + TagItemType.BEST_PRACTICE + "'"
                + ") as t ON t.itemid = bp.id"
                + " WHERE bp.id = ?"
                + " GROUP BY bp.id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(bpItemId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String[] rsTags = rs.getArray("tags") != null ?
                        (String[]) rs.getArray("tags").getArray() : new String[0];
                return Optional.of(BestPracticesItem.builder()
                        .id((UUID) rs.getObject("id"))
                        .name(rs.getString("name"))
                        .type(BestPracticesItem.BestPracticeType.fromString(rs.getString("type")))
                        .value(rs.getString("value"))
                        .metadata(rs.getString("metadata"))
                        .updatedAt(rs.getLong("updatedat"))
                        .createdAt(rs.getLong("createdat"))
                        .tags(rsTags.length > 0 ? Arrays.asList(rsTags)
                                : Collections.emptyList())
                        .build());
            }
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<BestPracticesItem> list(String company, Integer pageNumber,
                                                  Integer pageSize)
            throws SQLException {
        return list(company, null, null, pageNumber, pageSize);
    }

    @SuppressWarnings("rawtypes")
    public DbListResponse<BestPracticesItem> list(String company, List<Integer> tagIds, String name,
                                                  Integer pageNumber, Integer pageSize)
            throws SQLException {
        String bpCriteria = " WHERE";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagIds)) {
            bpCriteria += " ? && ANY(t.tags)";
            values.add(tagIds);
        }
        if (StringUtils.isNotEmpty(name)) {
            bpCriteria += (values.size() == 0) ? " bp.name ILIKE ?" : " AND bp.name ILIKE ?";
            values.add(name + "%");
        }
        if (values.size() == 0) {
            bpCriteria = "";
        }
        String SQL = "SELECT bp.id,"
                + " bp.name,"
                + " bp.type,"
                + " bp.value,"
                + " bp.metadata,"
                + " bp.updatedat,"
                + " bp.createdat,"
                + " t.tags"
                + " FROM " + company + ".bpracticesitems as bp"
                + " LEFT OUTER JOIN ("
                + " SELECT array_remove(array_agg(ti.tagid), NULL)::text[] as tags,"
                + " ti.itemid::uuid"
                + " FROM " + company + ".tagitems as ti"
                + " WHERE ti.itemtype = '" + TagItemType.BEST_PRACTICE + "'"
                + " GROUP BY ti.itemid ) as t ON t.itemid = bp.id"
                + bpCriteria
                + " ORDER BY bp.updatedat DESC "
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*)"
                + " FROM ( SELECT"
                + " bp.id, t.tags FROM "
                + company + ".bpracticesitems as bp"
                + " LEFT OUTER JOIN ("
                + " SELECT array_remove(array_agg(ti.tagid), NULL)::text[] as tags,"
                + " ti.itemid::uuid"
                + " FROM " + company + ".tagitems as ti"
                + " WHERE ti.itemtype = '" + TagItemType.BEST_PRACTICE + "'"
                + " GROUP BY ti.itemid ) as t ON t.itemid = bp.id"
                + bpCriteria
                + " ) AS counting";
        List<BestPracticesItem> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                if (obj instanceof List) {
                    obj = conn.createArrayOf("int", ((List) obj).toArray());
                }
                pstmt.setObject(i, obj);
                pstmt2.setObject(i, obj);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String[] rsTags = rs.getArray("tags") != null
                        ? (String[]) rs.getArray("tags").getArray()
                        : new String[0];
                retval.add(BestPracticesItem.builder()
                        .id((UUID) rs.getObject("id"))
                        .name(rs.getString("name"))
                        .type(BestPracticesItem.BestPracticeType.fromString(rs.getString("type")))
                        .value(rs.getString("value"))
                        .metadata(rs.getString("metadata"))
                        .createdAt(rs.getLong("createdat"))
                        .updatedAt(rs.getLong("updatedat"))
                        .tags(rsTags.length > 0 ? Arrays.asList(rsTags)
                                : Collections.emptyList())
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    public Optional<BestPracticesItem> deleteAndReturn(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".bpracticesitems WHERE id = ? RETURNING *";
        String SQL2 = "DELETE FROM " + company + ".tagitems WHERE itemid = ? " +
                "AND itemtype = '" + TagItemType.BEST_PRACTICE + "' RETURNING tagid";
        //no RETURN_GENERATED_KEYS because that breaks stuff.
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(SQL2)) {
            pstmt.setObject(1, UUID.fromString(id));
            pstmt.executeQuery();
            ResultSet rs = pstmt.getResultSet();
            if (rs.next()) {
                pstmt2.setObject(1, id); // the tagitems table stores itemids as varchar / string.
                pstmt2.executeQuery();
                ResultSet rs2 = pstmt2.getResultSet();
                List<String> tagIds = new ArrayList<>();
                while (rs2.next()) {
                    tagIds.add(rs2.getString("tagid"));
                }
                return Optional.of(BestPracticesItem.builder()
                        .id((UUID) rs.getObject("id"))
                        .type(BestPracticesItem.BestPracticeType.fromString(rs.getString("type")))
                        .tags(tagIds)
                        .name(rs.getString("name"))
                        .value(rs.getString("value"))
                        .updatedAt(rs.getLong("updatedat"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        }
        return Optional.empty();
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        if (CollectionUtils.isNotEmpty(ids)) {
            String SQL = "DELETE FROM " + company + ".bpracticesitems WHERE id IN (:ids) ";
            String SQL2 = "DELETE FROM " + company + ".tagitems WHERE itemid IN (:ids) " +
                    "AND itemtype = '" + TagItemType.BEST_PRACTICE + "'";
            Map<String, Object> params1 = Map.of("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
            Map<String, Object> params2 = Map.of("ids", ids);
            int rowsDeleted = template.update(SQL, params1);
            if (rowsDeleted > 0) {
                template.update(SQL2, params2);
                return rowsDeleted;
            }
        }
        return 0;
    }

    //we gotta do the other delete because we might need to cleanup uploaded files too
    @Override
    public Boolean delete(String company, String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".bpracticesitems(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    type VARCHAR NOT NULL,\n" +
                "    value VARCHAR,\n" +
                "    metadata VARCHAR,\n" +
                "    updatedat BIGINT DEFAULT extract(epoch from now()),\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
            return true;
        }
    }
}
