package io.levelops.api.services;

import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@SuppressWarnings("unused")
public class TagItemService {
    private static final int PAGE_SIZE = 500;
    private final TagItemDBService dbService;

    @Autowired
    public TagItemService(TagItemDBService dbService) {
        this.dbService = dbService;
    }

    public List<String> batchInsert (final String company, final UUID itemId, final TagItemMapping.TagItemType tagItemType, final List<String> tags) throws SQLException {
        if(CollectionUtils.isEmpty(tags)){
            return Collections.emptyList();
        }
        List<TagItemMapping> tagBPMappings = tags.stream()
                .map(tag -> TagItemMapping.builder()//
                        .tagId(tag)
                        .itemId(itemId.toString())
                        .tagItemType(tagItemType)
                        .build())
                .collect(Collectors.toList());
        return dbService.batchInsert(company, tagBPMappings);
    }

    public Boolean deleteTagsForItem(final String company, final UUID itemId, final TagItemMapping.TagItemType tagItemType) throws SQLException {
        return dbService.deleteTagsForItem(company, tagItemType.toString(), itemId.toString());
    }

    /*
    Input - List of Item Ids
    Output - Map<String,List<String>> -> key is Item Id, value is Tag Values List
     */
    public Map<String,List<String>> batchGetTagIdsForItemIds(final String company, final TagItemMapping.TagItemType tagItemType, final List<String> itemIds) throws SQLException {
        boolean keepFetching = true;
        int pageNumber =0;
        List<Pair<String, String>> results = new ArrayList<>();
        while (keepFetching) {
            DbListResponse<Pair<String, String>> listResponse = dbService.listTagIdsForItem(company, tagItemType, itemIds, pageNumber, PAGE_SIZE);
            results.addAll(listResponse.getRecords());
            keepFetching = (results.size() < listResponse.getTotalCount()) ? true:false;
        }
        return results.stream().collect(
                Collectors.groupingBy(Pair::getLeft, HashMap::new,
                        Collectors.mapping(Pair::getRight, Collectors.toList()))
        );
    }
}
