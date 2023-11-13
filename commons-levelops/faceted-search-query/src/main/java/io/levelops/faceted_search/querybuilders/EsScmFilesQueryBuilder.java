package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmAggService.FILES_PARTIAL_MATCH_COLUMNS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.EXCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.INCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.MAX_PAGE_SIZE;
import static io.levelops.faceted_search.utils.EsUtils.getNestedQuery;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTimeinMills;
import static io.levelops.faceted_search.utils.EsUtils.getRegex;
import static io.levelops.faceted_search.utils.EsUtils.getTimeRangeInMillis;
import static io.levelops.faceted_search.utils.EsUtils.getWildCardQuery;

public class EsScmFilesQueryBuilder {

    public static Map<String, List<Query>> buildQueryConditionsForFiles(ScmFilesFilter filesFilter) {

        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();

        createFileFilterIncludesCondition(filesFilter, includesQueryConditions);
        createFileFilterExcludesCondition(filesFilter, excludesQueryConditions);
        createCommitFilterRangeCondition(filesFilter, includesQueryConditions);

        if (MapUtils.isNotEmpty(filesFilter.getPartialMatch())) {
            getPartialMatch(filesFilter.getPartialMatch(), includesQueryConditions);
        }


        return Map.of(INCLUDE_CONDITIONS, includesQueryConditions,
                EXCLUDE_CONDITIONS, excludesQueryConditions);

    }

    private static void getPartialMatch(Map<String, Map<String, String>> partialMatch, List<Query> queries) {

        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (FILES_PARTIAL_MATCH_COLUMNS.contains(key)) {
                switch (key) {
                    case "repo_id":
                        key = "c_repo_id";
                        break;
                    case "project":
                        key = "c_project";
                        break;
                    case "filename":
                        key = "c_files.file_name";
                        break;
                }
                getRegex(begins, ends, contains, key, queries);
            }
        }

    }

    private static void createCommitFilterRangeCondition(ScmFilesFilter filesFilter, List<Query> includesQueryConditions) {
        if (filesFilter.getCommitStartTime() != null && filesFilter.getCommitEndTime() != null)  {
            ImmutablePair<Long, Long> commitRange = getTimeRangeInMillis(filesFilter.getCommitStartTime(), filesFilter.getCommitEndTime());
            includesQueryConditions.add(getRangeQueryForTimeinMills("c_committed_at", commitRange));
        }
    }

    private static void createFileFilterExcludesCondition(ScmFilesFilter filesFilter, List<Query> excludesQueryConditions) {


        if (CollectionUtils.isNotEmpty(filesFilter.getExcludeRepoIds())) {
            excludesQueryConditions.add(getNestedQuery("c_files", "c_files.repo", filesFilter.getRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filesFilter.getExcludeProjects())) {
            excludesQueryConditions.add(getNestedQuery("c_files", "c_files.project", filesFilter.getProjects()));
        }
    }

    private static void createFileFilterIncludesCondition(ScmFilesFilter filesFilter, List<Query> includesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filesFilter.getIntegrationIds())) {
            includesQueryConditions.add(getNestedQuery("c_files", "c_files.integration_id", filesFilter.getIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(filesFilter.getRepoIds())) {
            includesQueryConditions.add(getNestedQuery("c_files", "c_files.repo", filesFilter.getRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filesFilter.getProjects())) {
            includesQueryConditions.add(getNestedQuery("c_files", "c_files.project", filesFilter.getProjects()));
        }
        if(StringUtils.isNotEmpty(filesFilter.getFilename())){
            includesQueryConditions.add(getNestedQuery("c_files", "c_files.file_name", List.of(filesFilter.getFilename())));
        }

    }

    public static Map<String, Aggregation> buildAggsConditionForFiles(ScmFilesFilter filesFilter, Integer page, Integer pageSize){

        Map<String, Aggregation> aggConditions = new HashMap<>();

        int pageLength = pageSize == null ? MAX_PAGE_SIZE : pageSize;
        if(StringUtils.isEmpty(filesFilter.getModule())) {

            Script moduleScript = Script.of(s -> s.inline(i -> i
                    .source("def module = doc['c_files.file_name'].value; def repo = doc['c_files.repo'].value; def project = doc['c_files.project'].value; if (module != null) { int firstSlashIndex = module.indexOf('/'); if (firstSlashIndex > 0) { return module.substring(0, firstSlashIndex)+'#'+repo+'#'+project;}} return module+'#'+repo+'#'+project;")
            ));

            aggConditions.put("across_files", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                    .aggregations(Map.of( "across_module", Aggregation.of(a1 -> a1.terms(t -> t.script(moduleScript).size(pageLength))
                    ),  "total_count", Aggregation.of(a2 -> a2.cardinality(c ->c.script(moduleScript)))))

            ));
        }else{
            String prefix = filesFilter.getModule();
            if(prefix.startsWith("/"))
                prefix = prefix.substring(1);
            String modulePrefix = prefix;
            int startIndex = modulePrefix.length()+1;
            String scriptString = "def module = doc['c_files.file_name'].value; def repo = doc['c_files.repo'].value; def project = doc['c_files.project'].value; if (module != null && module.contains('"+modulePrefix+"') ) { module = module.substring("+startIndex+"); int firstSlashIndex = module.indexOf('/'); if (firstSlashIndex > 0) { return module.substring(0, firstSlashIndex)+'#'+repo+'#'+project;}} return module+'#'+repo+'#'+project";
            Script moduleScript = Script.of( s -> s.inline(i -> i
                    .source(scriptString)
            ));

            aggConditions.put("across_files", Aggregation.of(a -> a
                    .nested(n -> n.path("c_files"))
                    .aggregations(Map.of("wildcard_filter", Aggregation.of(a1 -> a1
                            .filter(f -> f.wildcard(getWildCardQuery("c_files.file_name", modulePrefix+"*").wildcard()))
                            .aggregations(Map.of( "across_module", Aggregation.of(a2 -> a2.terms(t -> t.script(moduleScript).size(pageLength))),
                                    "total_count", Aggregation.of(a2 -> a2.cardinality(c ->c.script(moduleScript)))))
                    )))));

        }

        return aggConditions;
    }

    public static Map<String, Aggregation> buildAggsConditionForListFiles(ScmFilesFilter filesFilter, Pair<String, SortOrder> sortOrder, Integer pageNumber, Integer pageSize){

        Map<String, Aggregation> aggConditions = new HashMap<>();

        String scriptString = "def id = doc['c_files.file_id'].value; def name = doc['c_files.file_name'].value; " +
                " def integration_id = doc['c_files.integration_id'].value; "+ " def repo = doc['c_files.repo'].value; "+
                " def project = doc['c_files.project'].value; def file_created_at = doc['c_files.file_created_at'].value;  " +
                "return id+'#'+name+'#'+integration_id+'#'+repo+'#'+project+'#'+file_created_at ";

        Script script = Script.of( s -> s.inline(i -> i
                .source(scriptString)
        ));

        Aggregation bucketSort = Aggregation.of(a -> a.bucketSort(b -> b
                .from(pageNumber * pageSize)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight()).nested(p -> p.path("c_files"))))
                .size(pageSize)));

        aggConditions.put("across_files", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                .aggregations(Map.of( "across_file_list", Aggregation.of(a1 -> a1.terms(t -> t.script(script).size(MAX_PAGE_SIZE))
                        .aggregations(Map.of("additions", Aggregation.of(a2 -> a2.sum(t -> t.field("c_files.addition"))),
                                "deletions", Aggregation.of(a2 -> a2.sum(t -> t.field("c_files.deletion"))),
                                "changes", Aggregation.of(a2 -> a2.sum(t -> t.field("c_files.change"))),
                                "num_commits",  Aggregation.of(a2 -> a2.valueCount(t -> t.field("c_files.commit_sha"))),
                                "bucket_pagination", bucketSort)
                        ))))
                .aggregations(Map.of("total_count", Aggregation.of(a1 -> a1.cardinality(t -> t.script(script)))))
        ));

        return aggConditions;
    }
}
