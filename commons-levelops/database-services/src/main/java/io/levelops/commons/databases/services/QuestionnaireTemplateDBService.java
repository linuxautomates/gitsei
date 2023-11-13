package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

@Log4j2
@Service
public class QuestionnaireTemplateDBService extends DatabaseService<QuestionnaireTemplate> {
    private static final TagItemMapping.TagItemType ITEM_TYPE = TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE;
    private final NamedParameterJdbcTemplate template;
    private static final String QT_KB_MAPPINGS_INSERT = "INSERT INTO %s.questionnaire_template_bpracticesitem_mappings(questionnaire_template_id,bpracticesitem_id) VALUES(?,?)";
    private static final String QT_KB_MAPPINGS_DELETE = "DELETE FROM %s.questionnaire_template_bpracticesitem_mappings WHERE questionnaire_template_id = ?";

    @Autowired
    public QuestionnaireTemplateDBService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    // region getReferences
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(BestPracticesService.class);
    }
    // endregion

    // region checkIfUsedInQuestionnaires
    public Optional<List<String>> checkIfUsedInQuestionnaires(final String company, final String qId) throws SQLException {
        String SQL = "SELECT id FROM " + company + ".questionnaires WHERE ? = questionnaire_template_id LIMIT 10";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setObject(1, UUID.fromString(qId));

            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            List<String> questionnaires = new ArrayList<>();
            do {
                questionnaires.add(rs.getString("id"));
            }
            while (rs.next());
            return Optional.of(questionnaires);
        }
    }
    // endregion

    // region QT KB Mappings
    private void deleteQTKBMappings(PreparedStatement qtKBMappingsDeletePtst, UUID questionnaireTemplateId) throws SQLException {
        qtKBMappingsDeletePtst.setObject(1, questionnaireTemplateId);
        qtKBMappingsDeletePtst.execute();
    }
    private List<String> insertQTKBMappings(PreparedStatement qtKBMappingsInsertPtst, UUID questionnaireTemplateId, List<UUID> kbIds) throws SQLException {
        if(CollectionUtils.isEmpty(kbIds)) {
            return Collections.emptyList();
        }
        for(UUID currentKbId : kbIds){
            qtKBMappingsInsertPtst.setObject(1, questionnaireTemplateId);
            qtKBMappingsInsertPtst.setObject(2, currentKbId);
            qtKBMappingsInsertPtst.addBatch();
            qtKBMappingsInsertPtst.clearParameters();
        }
        qtKBMappingsInsertPtst.executeBatch();
        List<String> ids = new ArrayList<>();
        try(ResultSet rs = qtKBMappingsInsertPtst.getGeneratedKeys()) {
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
        }
        return ids;
    }
    // endregion

    // region Insert
    @Override
    public String insert(String company, QuestionnaireTemplate questionnaireTemplate) throws SQLException {
        String SQL = "INSERT INTO " + company + ".questionnaire_templates(name,lowriskboundary," +
                "midriskboundary,sections,risk_enabled) VALUES(?,?,?,?,?)";
        String qtKBMappingsInsertSql = String.format(QT_KB_MAPPINGS_INSERT, company);

        UUID questionnaireTemplateId = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement qtKBMappingsInsertPtst = conn.prepareStatement(qtKBMappingsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                pstmt.setString(1, questionnaireTemplate.getName());
                pstmt.setInt(2, questionnaireTemplate.getLowRiskBoundary());
                pstmt.setInt(3, questionnaireTemplate.getMidRiskBoundary());
                pstmt.setArray(4, conn.createArrayOf("uuid",
                        questionnaireTemplate.getSections().toArray(new UUID[0])));
                pstmt.setBoolean(5, BooleanUtils.isTrue(questionnaireTemplate.getRiskEnabled()));
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create questionnaire template!");
                }
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        questionnaireTemplateId = (UUID) rs.getObject(1);
                    }
                }
                insertQTKBMappings(qtKBMappingsInsertPtst, questionnaireTemplateId, questionnaireTemplate.getKbIds());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return questionnaireTemplateId.toString();
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, QuestionnaireTemplate questionnaireTemplate) throws SQLException {
        String qtKBMappingsDeleteSql = String.format(QT_KB_MAPPINGS_DELETE, company);
        String qtKBMappingsInsertSql = String.format(QT_KB_MAPPINGS_INSERT, company);

        String SQL = "UPDATE " + company + ".questionnaire_templates SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(questionnaireTemplate.getName())) {
            updates += "name = ?,";
            values.add(questionnaireTemplate.getName());
        }
        if (questionnaireTemplate.getSections() != null) {
            updates += "sections = ?,";
            values.add(questionnaireTemplate.getSections());
        }
        if (questionnaireTemplate.getLowRiskBoundary() != null) {
            updates += "lowriskboundary = ?,";
            values.add(questionnaireTemplate.getLowRiskBoundary());
        }
        if (questionnaireTemplate.getMidRiskBoundary() != null) {
            updates += "midriskboundary = ?,";
            values.add(questionnaireTemplate.getMidRiskBoundary());
        }
        if (questionnaireTemplate.getRiskEnabled() != null) {
            updates += "risk_enabled = ?,";
            values.add(BooleanUtils.isTrue(questionnaireTemplate.getRiskEnabled()));
        }
        //no updates
        if (values.size() == 0) {
            return false;
        }
        updates += "updated_at = ?";
        values.add(Instant.now().getEpochSecond());
        SQL = SQL + updates + condition;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement qtKBMappingsDeletePstmt = conn.prepareStatement(qtKBMappingsDeleteSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement qtKBMappingsInsertPstmt = conn.prepareStatement(qtKBMappingsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
             try {
                 for (int i = 1; i <= values.size(); i++) {
                     Object obj = values.get(i - 1);
                     if (obj instanceof List) {
                         obj = conn.createArrayOf("uuid", ((List) obj).toArray(new UUID[0]));
                     }
                     pstmt.setObject(i, obj);
                 }
                 // The last item is the id so it will be handled differently
                 pstmt.setObject(values.size() + 1, UUID.fromString(questionnaireTemplate.getId()));
                 int affectedRows = pstmt.executeUpdate();
                 // check the affected rows
                 if (affectedRows <= 0) {
                     throw new SQLException("Failed to update questionnaire template!");
                 }
                 deleteQTKBMappings(qtKBMappingsDeletePstmt, UUID.fromString(questionnaireTemplate.getId()));
                 insertQTKBMappings(qtKBMappingsInsertPstmt, UUID.fromString(questionnaireTemplate.getId()), questionnaireTemplate.getKbIds());
                 conn.commit();
                 return true;
             } catch (SQLException e) {
                 conn.rollback();
                 throw e;
             } finally {
                 conn.setAutoCommit(autoCommit);
             }

        }
    }
    // endregion

    // region Get
    @Override
    public Optional<QuestionnaireTemplate> get(String company, String questionnaireTemplateId) throws SQLException {
        DbListResponse<QuestionnaireTemplate> dbListResponse = listByFilter(company, 0, 100, Set.of(), Arrays.asList(UUID.fromString(questionnaireTemplateId)), null);
        if (dbListResponse.getCount() != 1) {
            return Optional.empty();
        }
        return Optional.of(dbListResponse.getRecords().get(0));
    }
    // endregion

    // region List
    public DbListResponse<QuestionnaireTemplate> listByFilter(final String company, Integer pageNumber, Integer pageSize,
                                                              final String name, List<UUID> ids, List<String> tagIds) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, Set.of(name), ids, tagIds);
    }

    public DbListResponse<QuestionnaireTemplate> listByFilter(final String company, Integer pageNumber, Integer pageSize,
                                                              final Set<String> names, List<UUID> ids, List<String> tagIds) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, names, ids, tagIds, null, null);
    }

    public DbListResponse<QuestionnaireTemplate> listByFilter(final String company, Integer pageNumber, Integer pageSize,
                                                              final Set<String> names, List<UUID> ids, List<String> tagIds, Long updatedAtStart, Long updatedAtEnd) throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(names)) {
            criteria += "name ILIKE ANY (?::text[]) ";
            values.add(new ArrayWrapper<>("varchar", names.stream().filter(StringUtils::isNotBlank).map(name -> (name + "%")).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria += (values.size() == 0) ? "" : "AND ";
            criteria += "qt.id = ANY(?::uuid[]) ";
            //new ArrayWrapper<>
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(tagIds)) {
            criteria += (values.size() == 0) ? "" : "AND ";
            criteria += "tag_ids && ? ";
            values.add(new ArrayWrapper<>("varchar", tagIds));
        }
        if (updatedAtStart != null) {
            criteria += ((values.size() == 0) ? "" : "AND ") + "updated_at > ? ";
            values.add(updatedAtStart);
        }
        if (updatedAtEnd != null) {
            criteria += ((values.size() == 0) ? "" : "AND ") + "updated_at < ? ";
            values.add(updatedAtEnd);
        }
        if (values.size() == 0) {
            criteria = " ";
        }
        String base = 
                  "FROM " + company + ".questionnaire_templates qt \n"
                + "LEFT OUTER JOIN ( \n"
                + "   SELECT ti.itemid::uuid, array_remove(array_agg(ti.tagid), NULL)::varchar[] as tag_ids \n"
                + "   FROM " + company + ".tagitems as ti \n"
                + "   WHERE ti.itemtype = '" + ITEM_TYPE + "' \n"
                + "   GROUP BY ti.itemid ) as t ON t.itemid = qt.id \n"
                + "LEFT OUTER JOIN ( \n"
                + "   SELECT mp.questionnaire_template_id::uuid, array_remove(array_agg(mp.bpracticesitem_id), NULL)::uuid[] as kb_ids \n"
                + "   FROM " + company + ".questionnaire_template_bpracticesitem_mappings as mp \n"
                + "   GROUP BY mp.questionnaire_template_id) as m ON m.questionnaire_template_id = qt.id \n";
                
        String SQL = "SELECT \n"
                    + "   qt.id,\n"
                    + "   qt.name,\n"
                    + "   qt.updated_at,\n"
                    + "   qt.created_at,\n"
                    + "   qt.lowriskboundary,\n"
                    + "   qt.midriskboundary,\n"
                    + "   qt.sections,\n"
                    + "   qt.risk_enabled,\n"
                    + "   tag_ids,\n"
                    + "   kb_ids \n" 
                    + base + criteria + "ORDER BY qt.updated_at DESC LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);

        String countSQL = "SELECT COUNT(*) \n" + base + criteria;

        List<QuestionnaireTemplate> retval = new ArrayList<>();
        Integer totCount = 0;
        log.trace("Selecting with filters: {}", SQL);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            log.debug("Statement: {}", pstmt.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String[] rsTagIds = rs.getArray("tag_ids") != null
                            ? (String[]) rs.getArray("tag_ids").getArray()
                            : new String[0];
                    UUID[] kbIds = rs.getArray("kb_ids") != null
                            ? (UUID[]) rs.getArray("kb_ids").getArray()
                            : new UUID[0];
                    retval.add(QuestionnaireTemplate.builder()
                            .id(String.valueOf(rs.getObject("id")))
                            .name(rs.getString("name"))
                            .lowRiskBoundary(rs.getInt("lowriskboundary"))
                            .midRiskBoundary(rs.getInt("midriskboundary"))
                            .createdAt(rs.getLong("created_at"))
                            .updatedAt(rs.getLong("updated_at"))
                            .sections(Arrays.asList((UUID[]) rs.getArray("sections")
                                    .getArray()))
                            .riskEnabled(rs.getBoolean("risk_enabled"))
                            .tagIds(rsTagIds.length > 0 ? Arrays.asList(rsTagIds) : Collections.emptyList())
                            .kbIds(kbIds.length > 0 ? Arrays.asList(kbIds) : Collections.emptyList())
                            .build());
                }
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    try(ResultSet rs = pstmt2.executeQuery() ) {
                        if (rs.next()) {
                            totCount = rs.getInt("count");
                        }
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public DbListResponse<QuestionnaireTemplate> list(String company, Integer pageNumber,
                                                      Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, Set.of(), null, null);
    }
    // endregion

    // region Delete
    public Optional<QuestionnaireTemplate> deleteAndReturn(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".questionnaire_templates WHERE id = ? RETURNING *";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(id));
            pstmt.executeQuery();
            ResultSet rs = pstmt.getResultSet();
            if (rs.next()) {
                return Optional.of(QuestionnaireTemplate.builder()
                        .id(String.valueOf(rs.getObject("id")))
                        .name(rs.getString("name"))
                        .lowRiskBoundary(rs.getInt("lowriskboundary"))
                        .midRiskBoundary(rs.getInt("midriskboundary"))
                        .createdAt(rs.getLong("created_at"))
                        .updatedAt(rs.getLong("updated_at"))
                        .riskEnabled(rs.getBoolean("risk_enabled"))
                        .build());
            }
        }
        return Optional.empty();
    }

    public int bulkDeleteAndReturn(String company, List<String> ids) throws SQLException {
        int affectedRows = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream().map(UUID::fromString)
                    .collect(Collectors.toList()));
            String sql = "DELETE FROM " + company + ".questionnaire_templates WHERE id IN (:ids)";
            affectedRows = template.update(sql, params);
        }
        return affectedRows;
    }

    @Override
    public Boolean delete(String company, String id) {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region EnsureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqls = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".questionnaire_templates(\n" +
                "    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name              VARCHAR NOT NULL,\n" +
                "    lowriskboundary   INTEGER NOT NULL,\n" +
                "    midriskboundary   INTEGER NOT NULL,\n" +
                "    sections          UUID[],\n" +
                "    risk_enabled      BOOLEAN NOT NULL DEFAULT TRUE,\n" +
                "    updated_at        BIGINT DEFAULT extract(epoch from now()),\n" +
                "    created_at        BIGINT DEFAULT extract(epoch from now())\n" +
                ")",

                "CREATE INDEX IF NOT EXISTS questionnaire_templates_updated_at_idx ON " + company + ".questionnaire_templates (updated_at)",

                "CREATE TABLE IF NOT EXISTS " + company + ".questionnaire_template_bpracticesitem_mappings(\n" +
                "    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    questionnaire_template_id   UUID REFERENCES " + company + ".questionnaire_templates(id) ON DELETE CASCADE,\n" +
                "    bpracticesitem_id           UUID REFERENCES " + company + ".bpracticesitems(id) ON DELETE CASCADE\n" +
                ")",
                
                "CREATE INDEX IF NOT EXISTS questionnaire_template_bpracticesitem_mappings_questionnaire_template_id_idx on "
                + company + ".questionnaire_template_bpracticesitem_mappings (questionnaire_template_id)",
                
                "CREATE INDEX IF NOT EXISTS questionnaire_template_bpracticesitem_mappings_bpracticesitem_id_idx on "
                + company + ".questionnaire_template_bpracticesitem_mappings (bpracticesitem_id)");

        try (Connection conn = dataSource.getConnection()) {
            for (String currentSql : sqls) {
                try (PreparedStatement pstmt = conn.prepareStatement(currentSql)) {
                    pstmt.execute();
                }
            }
            return true;
        }
    }
    // endregion
}
