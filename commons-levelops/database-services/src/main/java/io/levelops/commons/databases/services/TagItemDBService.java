package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.models.DBMapResponse;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TagItemDBService extends DatabaseService<TagItemMapping> {

    protected final NamedParameterJdbcTemplate template;

    @Autowired
    public TagItemDBService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    // region Get References
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(TagsService.class);
    }
    // endregion

    // region Insert
    /**
     * NOTE: this does not return id unless insert was successful.
     *
     * @param company
     * @param mappings
     * @return
     * @throws SQLException
     */
    public List<String> batchInsert(String company, List<TagItemMapping> mappings)
            throws SQLException {

        String SQL = "INSERT INTO " + company + ".tagitems(tagid,itemid,itemtype) " +
                "VALUES(?,?,?) ON CONFLICT DO NOTHING RETURNING id";
        // there should be no conflict. delete all before insert ideally.

        List<String> ids = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            int i = 0;
            for (TagItemMapping mapping : mappings) {
                pstmt.setInt(1, Integer.parseInt(mapping.getTagId()));
                pstmt.setString(2, mapping.getItemId().toString());
                pstmt.setString(3, mapping.getTagItemType().toString());

                pstmt.addBatch();
                pstmt.clearParameters();
                i++;
                if (i % 100 == 0) {
                    pstmt.executeBatch();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }
            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        return ids;
    }

    @Override
    public String insert(String company, TagItemMapping mapping) throws SQLException {
        List<String> results = batchInsert(company, List.of(mapping));
        return CollectionUtils.isNotEmpty(results) ? results.get(0) : null;
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, TagItemMapping mapping) throws SQLException {
        List<String> results = updateItemMappings(company, mapping.getTagItemType(), mapping.getItemId(), List.of(mapping));
        return CollectionUtils.isNotEmpty(results);
    }

    public List<String> updateItemMappings(final String company,
                                           final TagItemMapping.TagItemType itemType,
                                           final String itemId,
                                           final List<TagItemMapping> mappings) throws SQLException {
        deleteTagsForItem(company, itemType.toString(), itemId);
        return batchInsert(company, mappings);
    }
    // endregion

    @Override
    public Optional<TagItemMapping> get(String company, String id) {
        String SQL = "SELECT * FROM " + company + ".tagitems WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(id));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(TagItemMapping.builder()
                        .createdAt(rs.getLong("createdat"))
                        .id(rs.getString("id"))
                        .itemId(rs.getString("itemid"))
                        .tagId(rs.getString("tagid"))
                        .tagItemType(TagItemType.valueOf(rs.getString("itemtype")))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<Pair<String, String>> listTagIdsForItem(String company, TagItemMapping.TagItemType itemType,
                                                              List<String> itemIds, Integer pageNumber, Integer pageSize)
            throws SQLException {
        String SQL = "SELECT tim.tagid,tim.itemid,tim.itemtype,t.name as name FROM "
                + company + ".tagitems tim LEFT OUTER JOIN ( SELECT name,id FROM "
                + company + ".tags ) t ON tim.tagid = t.id WHERE tim.itemid = ANY(?) "
                + "AND tim.itemtype = ? LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (SELECT tim.tagid,tim.itemid," +
                "tim.itemtype,t.name as name FROM " + company + ".tagitems tim" +
                " LEFT OUTER JOIN ( SELECT name,id FROM " + company + ".tags ) t" +
                " ON tim.tagid = t.id WHERE tim.itemid = ANY(?) AND tim.itemtype = ? ) AS data";
        Integer totCount = 0;
        List<Pair<String, String>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(SQL);
                PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            Array itemIdArr = conn.createArrayOf("varchar", itemIds.toArray());
            pstmt.setObject(1, itemIdArr);
            pstmt.setString(2, itemType.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String itemId = rs.getObject("itemid").toString();
                String tagValue =rs.getString("name");
                results.add(new ImmutablePair<>(itemId, tagValue));
            }
            if (results.size() > 0) {
                totCount = results.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
                if (results.size() == pageSize) {
                    pstmt2.setArray(1, itemIdArr);
                    pstmt2.setString(2, itemType.toString());
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(results, totCount);
    }

    public DbListResponse<Tag> listTagsForItem(String company, TagItemMapping.TagItemType itemType,
                                                              String itemId, Integer pageNumber, Integer pageSize)
            throws SQLException {
        TagItemMapping mapping = TagItemMapping.builder().itemId(itemId).tagItemType(itemType).build();
        DBMapResponse<String, List<Tag>> response = listTagsForItems(company, List.of(mapping), pageNumber, pageSize);
        return DbListResponse.of(response.getRecords().get(itemId + itemType), response.getTotalCount());
    }

    public DBMapResponse<String, List<Tag>> listTagsForItems(String company, List<TagItemMapping> mappings,
                                                    Integer pageNumber, Integer pageSize)
            throws SQLException {
        if (CollectionUtils.isEmpty(mappings)){
            return DBMapResponse.of(Collections.emptyMap(), 0);
        }
        List<String> criteria = mappings.stream()
                                .map( item -> "('" + item.getItemId() + "','" + item.getTagItemType() + "')")
                                .collect(Collectors.toList());
        String mainQuery = "FROM " 
                            + company + ".tagitems tim " 
                            + "LEFT OUTER JOIN ( SELECT name, id FROM " + company + ".tags ) t ON tim.tagid = t.id " 
                        + "WHERE "
                            + "(tim.itemid, tim.itemtype) IN (" + String.join(",", criteria) + ") ";
        String SQL = "SELECT tim.tagid,tim.itemid,tim.itemtype,t.name as name " 
                + mainQuery
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) " + mainQuery;
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {

            ResultSet rs = pstmt.executeQuery();
            Map<String, List<Tag>> retval = new HashMap<>();
            while (rs.next()) {
                String key = rs.getString("itemid") + rs.getString("itemtype");
                var tags = retval.getOrDefault(key, new ArrayList<Tag>());
                tags.add(Tag.builder().id(rs.getString("tagid")).name(rs.getString("name")).build());
                retval.put(key, tags);
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
            return DBMapResponse.of(retval, totCount);
        }
    }

    public DbListResponse<String> listItemIdsForTagIds(String company, TagItemType itemType, List<String> tagIds,
                                                        Integer pageNumber, Integer pageSize) throws SQLException {
        if (CollectionUtils.isEmpty(tagIds) || itemType == null){
            return DbListResponse.of(Collections.emptyList(), 0);
        }
        List<String> criteria = tagIds.stream()
                                .map( tagId -> "(" + tagId + ",'" + itemType + "')")
                                .collect(Collectors.toList());
        String mainQuery = "FROM "
                            + company + ".tagitems "
                        + "WHERE "
                            + "(tagid, itemtype) IN (" + String.join(",", criteria) + ") "
                        + "GROUP BY "
                            + "itemid ";
        String SQL = "SELECT itemid " 
                + mainQuery
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(itemid) " + mainQuery;
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {

            ResultSet rs = pstmt.executeQuery();
            List<String> itemIds = new ArrayList<>();
            while (rs.next()) {
                itemIds.add(rs.getString("itemId"));
            }
            if (itemIds.size() > 0) {
                totCount = itemIds.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
                if (itemIds.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
            return DbListResponse.of(itemIds, totCount);
        }
        
    }

    @Override
    public DbListResponse<TagItemMapping> list(String company, Integer pageNumber,
                                               Integer pageSize) {
        throw new UnsupportedOperationException();
    }

    // region Delete
    public Boolean deleteTagsForItem(String company, String itemType, String itemId)
            throws SQLException {
        String SQL = "DELETE FROM " + company + ".tagitems WHERE itemtype = ? AND itemid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setString(1, itemType);
            pstmt.setString(2, itemId);
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int bulkDeleteTagsForItems(String company, String itemType, List<String> ids) {
        String SQL = "DELETE FROM " + company + ".tagitems WHERE itemtype = (:itemtype) AND itemid IN (:ids)";
        int rowsDeleted = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids, "itemtype", itemType);
            rowsDeleted = template.update(SQL, params);
        }
        return rowsDeleted;
    }

    @Override
    public Boolean delete(String company, String name) {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".tagitems(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    itemtype VARCHAR NOT NULL,\n" +
                "    itemid VARCHAR NOT NULL,\n" +
                "    tagid INTEGER NOT NULL REFERENCES " + company + ".tags(id) ON DELETE CASCADE,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now()),\n" +
                "    CONSTRAINT unq_ittyp_itid_tagid UNIQUE(itemtype,itemid,tagid)\n" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
            return true;
        }
    }
    // endregion

}