package io.levelops.commons.databases.services;

import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TagsService extends DatabaseService<Tag> {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TagsService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, Tag tag) throws SQLException {
        List<String> ids = insert(company, List.of(tag.getName()));
        if (ids != null && ids.size() > 0) {
            return ids.get(0);
        }
        return null;
    }

    public List<String> insert(String company, List<String> tagValues) throws SQLException {

        String SQL = "INSERT INTO " + company + ".tags(name) VALUES(?)";
        if (CollectionUtils.isEmpty(tagValues)) {
            throw new SQLException("Empty tag name provided.");
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            for (String tag : tagValues) {
                pstmt.setString(1, tag.toLowerCase());
                pstmt.addBatch();
            }

            int[] affectedRows = pstmt.executeBatch();
            // check the affected rows
            if (affectedRows.length > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    List<String> ids = Lists.newArrayList();
                    while (rs.next()) {
                        ids.add(rs.getString(1));
                    }
                    return ids;
                }
            }
        }
        return null;
    }

    @Override
    public Boolean update(String company, Tag tag) throws SQLException {
        String SQL = "UPDATE " + company + ".tags SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(tag.getName())) {
            updates = "name = ?";
            values.add(tag.getName().toLowerCase());
        }
        SQL = SQL + updates + condition;
        //no updates
        if (values.size() == 0) {
            return false;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
            }
            // id
            pstmt.setObject(values.size() + 1, Integer.parseInt(tag.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<Tag> get(String company, String tagId) {
        String SQL = "SELECT id,name,createdat FROM " + company + ".tags WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(tagId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(Tag.builder()
                        .createdAt(rs.getLong("createdat"))
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public List<String> forceGetTagIds(String company, List<String> tagValues) throws SQLException {
        // find ids
        List<Tag> tmp = findTagsByValues(company, tagValues);
        if (tmp.size() == tagValues.size()) {
            return tmp.stream().map(Tag::getId).collect(Collectors.toList());
        }
        // insert missing tags
        Set<String> existing = tmp.stream().map(Tag::getName).collect(Collectors.toSet());
        List<String> newTags = tagValues.stream().map(String::toLowerCase).filter(tag -> !existing.contains(tag)).collect(Collectors.toList());
        List<String> newIds = insert(company, newTags);
        // merge new and existing ids
        Set<String> ids = tmp.stream().map(Tag::getId).collect(Collectors.toSet());
        ids.addAll(newIds);
        return List.copyOf(ids);
    }

    public List<Tag> findTagsByValues(String company, List<String> tagValues) throws SQLException {
        if (CollectionUtils.isEmpty(tagValues)) {
            return Collections.emptyList();
        }
        String SQL = "SELECT id,name,createdat FROM " + company + ".tags"
                + " WHERE name = ANY(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            // pstmt.setObject(1, conn.createArrayOf("text", tagValues.toArray()));
            pstmt.setObject(1, conn.createArrayOf("text", tagValues.stream().map(String::toLowerCase).collect(Collectors.toList()).toArray()));
            ResultSet rs = pstmt.executeQuery();
            List<Tag> retval = new ArrayList<>();
            while (rs.next()) {
                retval.add(Tag.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
            return retval;
        }
    }

    public DbListResponse<Tag> listByFilter(String company,
                                            List<String> tagIds,
                                            String namePrefix,
                                            Integer pageNumber,
                                            Integer pageSize)
            throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(namePrefix)) {
            criteria += "name LIKE ? ";
            values.add(namePrefix.toLowerCase() + "%");
        }
        if (!CollectionUtils.isEmpty(tagIds)) {
            criteria += (values.size() == 0) ? "id = ANY(?::int[]) " : "AND id = ANY(?::int[]) ";
            values.add(tagIds.stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
        if (values.size() == 0)
            criteria = "";
        String SQL = "SELECT id,name,createdat FROM " + company + ".tags"
                + criteria + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<Tag> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM " + company + ".tags" + criteria;
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
                retval.add(Tag.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .createdAt(rs.getLong("createdat"))
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

    @Override
    public DbListResponse<Tag> list(String company, Integer pageNumber,
                                    Integer pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".tags WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.tags(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS tags_name_idx on {0}.tags (lower(name))");
        ddl.stream().map(sql -> MessageFormat.format(sql, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}