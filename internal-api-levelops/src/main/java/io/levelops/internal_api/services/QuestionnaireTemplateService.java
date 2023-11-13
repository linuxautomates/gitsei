package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.services.QuestionnaireTemplateDBService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QuestionnaireTemplateService {
    private static final TagItemMapping.TagItemType TAG_ITEM_TYPE = TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE;
    private final TagItemService tagItemService;
    private final QuestionnaireTemplateDBService dbService;

    @Autowired
    public QuestionnaireTemplateService(TagItemService tagItemService, QuestionnaireTemplateDBService dbService) {
        this.tagItemService = tagItemService;
        this.dbService = dbService;
    }

    public QuestionnaireTemplate create(final String company, final QuestionnaireTemplate questionnaireTemplate) throws SQLException {
        String id = dbService.insert(company, questionnaireTemplate);
        // Insert
        tagItemService.batchInsert(company, UUID.fromString(id), TAG_ITEM_TYPE, questionnaireTemplate.getTagIds());
        return questionnaireTemplate.toBuilder().id(id).build();
    }

    public Optional<QuestionnaireTemplate> read(final String company, final String id) throws SQLException {
        return dbService.get(company, id);
    }

    public QuestionnaireTemplate update(final String company, final UUID id, final QuestionnaireTemplate questionnaireTemplate) throws SQLException {
        // Assemble Questionnaire object
        if (questionnaireTemplate.getTagIds() != null) { // for testing only...
            // delete previous tag associations.
            tagItemService.deleteTagsForItem(company, id, TAG_ITEM_TYPE);
            tagItemService.batchInsert(company, id, TAG_ITEM_TYPE, questionnaireTemplate.getTagIds());
        }

        // Update
        if (!dbService.update(company, questionnaireTemplate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update questionnaire.");
        }
        return questionnaireTemplate;
    }

    public boolean delete(final String company, final String id) throws SQLException {
        var inUsed = dbService.checkIfUsedInQuestionnaires(company, id);
        if (inUsed.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete a questionnaire template that is linked to at least one questionnaire: " +
                    String.join(",", inUsed.get()));
        }
        // Delete db records
        if (dbService.deleteAndReturn(company, id).isEmpty()) {
            return false;
        }
        tagItemService.deleteTagsForItem(company, UUID.fromString(id), TAG_ITEM_TYPE);
        return true;
    }

    public int bulkDelete(final String company, final List<String> ids) throws SQLException {
        List<String> deletableIds = ids.stream().filter(id -> {
            try {
                var inUse = dbService.checkIfUsedInQuestionnaires(company, id);
                if (inUse.isPresent()) {
                    log.error("Cannot delete a questionnaire template that is linked to at least one questionnaire for id : " + id + " " +
                            String.join(",", inUse.get()));
                }
                return inUse.isEmpty();
            } catch (SQLException throwables) {
                log.error("Failed to get usages of questionnaire template {}", id, throwables);
                return true;
            }
        }).collect(Collectors.toList());

        // Delete db records
        int deletedRows = dbService.bulkDeleteAndReturn(company, deletableIds);
        tagItemService.bulkDeleteTagsForItem(company, deletableIds.stream().map(id -> UUID.fromString(id)).collect(Collectors.toList()), TAG_ITEM_TYPE);
        return deletedRows;
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<QuestionnaireTemplate> readList(final String company, int pageNumber, int pageSize, Map<String, Object> filters) throws SQLException {
        Set<String> partialNames = new HashSet<>();

        var tmpNames = (Collection<String>) ((Map<String, Object>) filters.getOrDefault("partial", Map.<String, Object>of())).get("names");

        String partialName = ((Map<String, String>) filters.getOrDefault("partial", Map.of())).get("name");
        if(Strings.isNotBlank(partialName)) {
            partialNames.add(partialName);
        }
        if(CollectionUtils.isNotEmpty(tmpNames)) {
            partialNames.addAll(tmpNames);
        }
        List<String> tagIds = (filters.get("tag_ids") != null) ?
                ((List<String>) filters.get("tag_ids")) : Collections.emptyList();
        List<UUID> ids = (filters.get("ids") != null) ?
                ((List<String>) filters.get("ids")).stream().map(UUID::fromString).collect(Collectors.toList()) : Collections.emptyList();
        
        Map<String, Integer> updateRange = (Map<String, Integer>) filters.getOrDefault("updated_at", Map.of());
        Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
        Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;
        return dbService.listByFilter(company, pageNumber, pageSize, partialNames, ids, tagIds, updatedAtStart, updatedAtEnd);
    }
}
