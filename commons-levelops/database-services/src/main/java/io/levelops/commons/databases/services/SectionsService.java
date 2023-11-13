package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.google.api.client.util.Strings;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.ActionMode;
import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class SectionsService extends DatabaseService<Section> {

    private ObjectMapper mapper;
    private final TagItemDBService tagItemDBService;

    @Autowired
    public SectionsService(DataSource dataSource, ObjectMapper mapper, TagItemDBService tagItemDBService) {
        super(dataSource);
        this.mapper = mapper;
        this.tagItemDBService = tagItemDBService;
    }

    // region Save Tags
    private void batchSaveTags(final String company, final TagItemType tagItemType, Map<UUID, List<String>>questionIdTagIdsMap) throws SQLException {
        List<TagItemMapping> tagItemMappings = questionIdTagIdsMap.entrySet().stream()
                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                .flatMap(e -> {
                    UUID questionId = e.getKey();
                    List<String> tagIds = e.getValue();
                    return tagIds.stream()
                            .map(tagId -> TagItemMapping.builder()
                                    .itemId(questionId.toString())
                                    .tagId(tagId)
                                    .tagItemType(tagItemType)
                                    .build());
                }).collect(Collectors.toList());
        tagItemDBService.batchInsert(company, tagItemMappings);
    }

    private void batchDeleteTags(final String company, final TagItemType tagItemType, List<UUID>questionIds) throws SQLException {
        for(UUID questionId : questionIds){
            tagItemDBService.deleteTagsForItem(company, tagItemType.toString(), questionId.toString());
        }
    }

    private void batchUpdateTags(final String company, final TagItemType tagItemType, Map<UUID, List<String>>questionIdTagIdsMap) throws SQLException {
        batchDeleteTags(company, tagItemType, questionIdTagIdsMap.keySet().stream().collect(Collectors.toList()));
        batchSaveTags(company, tagItemType, questionIdTagIdsMap);
    }
    // endregion
    // region Insert
    @Override
    public String insert(String company, Section qn) throws SQLException {
        String SQL = "INSERT INTO " + company + ".sections(type,attachment,name,description) VALUES(?,?,?,?)";
        String SQL2 = "INSERT INTO " + company + ".questions(name,type,verificationmode,section_id,options," +
                "severity,verifiable,number,required,id) VALUES(?,?,?,?,to_json(?::json),?,?,?,?,?)";
        UUID sectionId = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt2 = conn.prepareStatement(SQL2, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                pstmt.setObject(1, String.valueOf(qn.getType()));
                if (StringUtils.isNotEmpty(qn.getAttachment())) {
                    pstmt.setObject(2, qn.getAttachment());
                } else {
                    pstmt.setNull(2, Types.VARCHAR);
                }
                pstmt.setString(3, qn.getName());
                pstmt.setString(4, qn.getDescription());
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create section!");
                }
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to create section!");
                    }
                    sectionId = (UUID) rs.getObject(1);
                }
                if (qn.getQuestions() != null && qn.getQuestions().size() != 0) {
                    Map<UUID, List<String>> questionIdTagIdsMap = new HashMap<>();
                    for (Question question : qn.getQuestions()) {
                        UUID questionId = Strings.isNullOrEmpty(question.getId()) ? UUID.randomUUID() : UUID.fromString(question.getId());
                        pstmt2.setString(1, question.getName());
                        pstmt2.setString(2, question.getType());
                        pstmt2.setString(3, String.valueOf(question.getVerificationMode()));
                        pstmt2.setObject(4, sectionId);
                        pstmt2.setString(5, mapper.writeValueAsString(question.getOptions()));
                        pstmt2.setString(6, String.valueOf(question.getSeverity()));
                        pstmt2.setBoolean(7, question.getVerifiable());
                        pstmt2.setInt(8, question.getNumber());
                        pstmt2.setBoolean(9, BooleanUtils.isTrue(question.getRequired()));
                        pstmt2.setObject(10, questionId);
                        pstmt2.addBatch();
                        pstmt2.clearParameters();
                        questionIdTagIdsMap.put(questionId, question.getTagIds());
                    }
                    pstmt2.executeBatch();
                    batchSaveTags(company, TagItemType.QUESTION, questionIdTagIdsMap);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to convert questions to string in sectionsservice", e);
        }
        return sectionId.toString();
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, Section qn) throws SQLException {
        String SQL = "UPDATE " + company + ".sections SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<String> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(qn.getName())) {
            updates = "name = ?";
            values.add(qn.getName());
        }
        if (StringUtils.isNotEmpty(qn.getDescription())) {
            updates = StringUtils.isEmpty(updates) ? "description = ?" : updates + ", description = ?";
            values.add(qn.getDescription());
        }
        if (qn.getType() != null) {
            updates = StringUtils.isEmpty(updates) ? "type = ?" : updates + ", type = ?";
            values.add(qn.getType().toString());
        }
        if (StringUtils.isNotEmpty(qn.getAttachment())) {
            updates = StringUtils.isEmpty(updates) ? "attachment = ?" : updates + ", attachment = ?";
            values.add(qn.getAttachment());
        }
        UUID qnId = UUID.fromString(qn.getId());
        SQL = SQL + updates + condition;
        String deleteQuestionsQuery = "DELETE FROM " + company + ".questions WHERE section_id = ?";
        String insertQuestionQuery = "INSERT INTO " + company + ".questions(id,name,type,"
                + "verificationmode,section_id,options, severity, verifiable, number,required)"
                + " VALUES(?,?,?,?,?,to_json(?::json),?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement deleteQuestionPstmt = conn.prepareStatement(deleteQuestionsQuery);
             PreparedStatement insertQuestionPstmt = conn.prepareStatement(insertQuestionQuery)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (values.size() != 0) {
                    for (int i = 1; i <= values.size(); i++) {
                        pstmt.setString(i, values.get(i - 1));
                    }
                    pstmt.setObject(values.size() + 1, qnId);
                    pstmt.execute();
                }
                if (qn.getQuestions() != null) {
                    deleteQuestionPstmt.setObject(1, qnId);
                    deleteQuestionPstmt.execute();
                    Map<UUID, List<String>> questionIdTagIdsMap = new HashMap<>();
                    for (Question question : qn.getQuestions()) {
                        UUID questionId = StringUtils.isEmpty(question.getId()) ?
                                UUID.randomUUID() : UUID.fromString(question.getId());
                        insertQuestionPstmt.setObject(1, questionId);
                        insertQuestionPstmt.setString(2, question.getName());
                        insertQuestionPstmt.setString(3, question.getType());
                        insertQuestionPstmt.setString(4, String.valueOf(question.getVerificationMode()));
                        insertQuestionPstmt.setObject(5, qnId);
                        insertQuestionPstmt.setString(6,
                                mapper.writeValueAsString(question.getOptions()));
                        insertQuestionPstmt.setString(7, String.valueOf(question.getSeverity()));
                        insertQuestionPstmt.setBoolean(8, question.getVerifiable());
                        insertQuestionPstmt.setInt(9, question.getNumber());
                        insertQuestionPstmt.setBoolean(10, BooleanUtils.isTrue(question.getRequired()));
                        insertQuestionPstmt.addBatch();
                        insertQuestionPstmt.clearParameters();
                        questionIdTagIdsMap.put(questionId, question.getTagIds());
                    }
                    insertQuestionPstmt.executeBatch();
                    batchUpdateTags(company, TagItemType.QUESTION, questionIdTagIdsMap);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to convert questions to string in sectionsservice.",
                    e);
        }
        return true;
    }
    // endregion

    // region checkIfUsedInQuestionnaireTemplates
    public Optional<List<String>> checkIfUsedInQuestionnaireTemplates(final String company, final String qnId) throws SQLException {
        String SQL = "SELECT id,name FROM " + company + ".questionnaire_templates WHERE ? = ANY(sections) LIMIT 10";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setObject(1, UUID.fromString(qnId));

            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            List<String> questionnaires = new ArrayList<>();
            do {
                questionnaires.add(rs.getString("name"));
            }
            while (rs.next());
            return Optional.of(questionnaires);
        }
    }
    // endregion

    // region Get & List
    @Override
    public Optional<Section> get(String company, String sectionId) throws SQLException {
        var results = getBatch(company, Collections.singletonList(UUID.fromString(sectionId)));
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    public List<Section> getBatch(String company, List<UUID> ids) throws SQLException {
        String SQL = "SELECT s.id,s.name,s.description,s.createdat,s.type,s.attachment,t.tags,\n"
                + "qv.id as q_id,qv.name as q_name,qv.section_id as q_section_id,qv.type as q_type,qv.options as q_options,qv.verifiable as q_verifiable,qv.verificationmode as q_verificationmode,qv.createdat as q_createdat,qv.severity as q_severity,qv.number as q_number,qv.required as q_required,q_tag_ids\n"
                + "FROM " + company + ".sections s\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT q.id,q.name,q.section_id,q.type,q.options,q.verifiable,q.verificationmode,q.createdat,q.severity,q.number,q.required,q_tag_ids\n"
                + "FROM " + company + ".questions q\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT qti.itemid::uuid, array_remove(array_agg(qti.tagid), NULL)::varchar[] as q_tag_ids\n"
                + "FROM " + company + ".tagitems as qti WHERE qti.itemtype = 'QUESTION'\n"
                + "GROUP BY qti.itemid ) as qt ON qt.itemid = q.id\n"
                + ") as qv ON qv.section_id = s.id\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT array_remove(array_agg(ti.tagid), NULL)::varchar[] as tags,\n"
                + "ti.itemid::uuid FROM " + company + ".tagitems as ti\n"
                + "WHERE ti.itemtype = '" + TagItemType.SECTION + "'\n"
                + "GROUP BY ti.itemid ) as t ON t.itemid = s.id\n"
                + "WHERE s.id = ANY(?::uuid[])\n"
                + "ORDER BY id";

        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, Question.Option.class);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);) {

            pstmt.setArray(1, conn.createArrayOf("uuid", ids.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();

            Map<UUID,Section> sections = new HashMap<>();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                if(!sections.containsKey(id)){
                    String[] rsTags = rs.getArray("tags") != null
                            ? (String[]) rs.getArray("tags").getArray()
                            : new String[0];
                    Section section = Section.builder()
                            .id(String.valueOf(id))
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .createdAt(rs.getLong("createdat"))
                            .questions(new ArrayList<>())
                            .attachment(rs.getString("attachment"))
                            .type(Section.Type.fromString(
                                    rs.getString("type")))
                            .tags(rsTags.length > 0 ?
                                    Arrays.asList(rsTags) : Collections.emptyList())
                            .build();
                    sections.put(id,section);
                }
                String options = rs.getString("q_options");
                if (options != null) {
                    String[] rsTagIds = rs.getArray("q_tag_ids") != null
                            ? (String[]) rs.getArray("q_tag_ids").getArray()
                            : new String[0];
                    Question question = Question.builder()
                            .name(rs.getString("q_name"))
                            .options(mapper.readValue(options, type))
                            .verifiable(rs.getBoolean("q_verifiable"))
                            .verificationMode(ActionMode.fromString(rs.getString("q_verificationmode")))
                            .type(rs.getString("q_type"))
                            .sectionId(rs.getObject("q_section_id").toString())
                            .id(rs.getObject("q_id").toString())
                            .severity(Severity.fromString(rs.getString("q_severity")))
                            .number(rs.getInt("q_number"))
                            .required(rs.getBoolean("q_required"))
                            .tagIds(rsTagIds.length > 0 ? Arrays.asList(rsTagIds) : Collections.emptyList())
                            .build();
                    Section section = sections.get(id);
                    Section updatedSection = section.toBuilder().question(question).build();
                    sections.put(id, updatedSection);
                }
            }
            return sections.values().stream().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
    @Override
    public DbListResponse<Section> list(String company, Integer pageNumber,
                                        Integer pageSize) throws SQLException {
        return listByTagIds(company, null, null, pageNumber, pageSize,null);
    }
    public DbListResponse<Section> listByTagIds(String company, List<Integer> tagIds, String name, Integer pageNumber, Integer pageSize, List<String> questionTagIds)
            throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagIds)) {
            criteria += (values.size() ==0) ? "" : "AND ";
            criteria += " ? && ANY(t.tags) ";
            values.add(new ArrayWrapper<>("int", tagIds));
        }
        if (StringUtils.isNotEmpty(name)) {
            criteria += (values.size() == 0) ? " q.name ILIKE ?" : " AND q.name ILIKE ?";
            values.add(name + "%");
        }
        if(CollectionUtils.isNotEmpty(questionTagIds)){
            criteria += (values.size() ==0) ? "" : "AND ";
            criteria += "q_tag_ids && ? ";
            values.add(new ArrayWrapper<>("varchar", questionTagIds));
        }
        if (values.size() == 0) {
            criteria = "";
        }

        String SQL = "SELECT DISTINCT (s.id)\n"
                + "FROM " + company + ".sections s\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT q.id,q.name,q.section_id,q.type,q.options,q.verifiable,q.verificationmode,q.createdat,q.severity,q.number,q.required,q_tag_ids\n"
                + "FROM " + company + ".questions q\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT qti.itemid::uuid, array_remove(array_agg(qti.tagid), NULL)::varchar[] as q_tag_ids\n"
                + "FROM " + company + ".tagitems as qti WHERE qti.itemtype = 'QUESTION'\n"
                + "GROUP BY qti.itemid ) as qt ON qt.itemid = q.id\n"
                + ") as qv ON qv.section_id = s.id\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT array_remove(array_agg(ti.tagid), NULL)::varchar[] as tags,\n"
                + "ti.itemid::uuid FROM " + company + ".tagitems as ti\n"
                + "WHERE ti.itemtype = '" + TagItemType.SECTION + "'\n"
                + "GROUP BY ti.itemid ) as t ON t.itemid = s.id\n"
                + criteria + " \n"
                + "ORDER BY id\n"
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(DISTINCT (s.id))\n"
                + "FROM " + company + ".sections s\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT q.id,q.name,q.section_id,q.type,q.options,q.verifiable,q.verificationmode,q.createdat,q.severity,q.number,q.required,q_tag_ids\n"
                + "FROM " + company + ".questions q\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT qti.itemid::uuid, array_remove(array_agg(qti.tagid), NULL)::varchar[] as q_tag_ids\n"
                + "FROM " + company + ".tagitems as qti WHERE qti.itemtype = 'QUESTION'\n"
                + "GROUP BY qti.itemid ) as qt ON qt.itemid = q.id\n"
                + ") as qv ON qv.section_id = s.id\n"
                + "LEFT OUTER JOIN (\n"
                + "SELECT array_remove(array_agg(ti.tagid), NULL)::varchar[] as tags,\n"
                + "ti.itemid::uuid FROM " + company + ".tagitems as ti\n"
                + "WHERE ti.itemtype = '" + TagItemType.SECTION + "'\n"
                + "GROUP BY ti.itemid ) as t ON t.itemid = s.id\n"
                + criteria + " \n"
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);

        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for(int i=0; i< values.size(); i++){
                Object obj = DBUtils.processArrayValues(conn,values.get(i));
                pstmt.setObject(i+1, obj);
                pstmt2.setObject(i+1, obj);
            }
            Set<UUID> sectionIds = new HashSet<>();
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                sectionIds.add(id);
            }
            if (sectionIds.size() > 0) {
                totCount = sectionIds.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (sectionIds.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
            List<Section> sections = getBatch(company, sectionIds.stream().collect(Collectors.toList()));
            return DbListResponse.of(sections, totCount);
        }
    }
    // endregion

    //region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        UUID sectionId = UUID.fromString(id);
        String SELECT_QUESTIONS_SQL = "SELECT id FROM " + company + ".questions WHERE section_id = ?";
        String SQL2 = "DELETE FROM " + company + ".sections WHERE id = ?";
        String SQL3 = "DELETE FROM " + company + ".tagitems WHERE itemid = ? " +
                "AND itemtype = '" + TagItemType.SECTION + "'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt1 = conn.prepareStatement(SELECT_QUESTIONS_SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(SQL2);
             PreparedStatement pstmt3 = conn.prepareStatement(SQL3)) {
            pstmt1.setObject(1, sectionId);
            pstmt2.setObject(1, sectionId);
            pstmt3.setObject(1, id); // tagitems stores itemid as varchar / string
            List<UUID> questionIds = new ArrayList<>();
            ResultSet rs = pstmt1.executeQuery();
            while (rs.next()) {
                questionIds.add((UUID) rs.getObject("id"));
            }

            if (pstmt2.executeUpdate() > 0) {
                pstmt3.executeUpdate();
                batchDeleteTags(company, TagItemType.QUESTION, questionIds);
                return true;
            }
        }
        return false;
    }
    // endregion

    // region EnsureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".sections(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    type VARCHAR NOT NULL,\n" +
                "    attachment VARCHAR,\n" +
                "    description VARCHAR NOT NULL,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sql2 = "CREATE TABLE IF NOT EXISTS " + company + ".questions(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    type VARCHAR NOT NULL,\n" +
                "    severity VARCHAR NOT NULL,\n" +
                "    options JSONB NOT NULL,\n" +
                "    verifiable BOOLEAN NOT NULL,\n" +
                "    verificationmode VARCHAR NOT NULL,\n" +
                "    number INT NOT NULL,\n" +
                "    section_id UUID REFERENCES " + company + ".sections(id) ON DELETE CASCADE, \n" +
                "    required BOOLEAN NOT NULL DEFAULT FALSE,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sqlIndexCreation = "CREATE INDEX IF NOT EXISTS questions_section_id_idx on "
                + company + ".questions (section_id)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmt1 = conn.prepareStatement(sql2);
             PreparedStatement pstmt2 = conn.prepareStatement(sqlIndexCreation)) {
            pstmt.execute();
            pstmt1.execute();
            pstmt2.execute();
            return true;
        }
    }
    // endregion
}
