package io.levelops.aggregations.functions;

import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.JSONB_ARRAY_FIELD;
import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.STRING;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.EXACT_MATCH;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.NON_NULL_CHECK;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.NOT_EQUALS;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.NULL_CHECK;

@Log4j2
@SuppressWarnings("unused")
public class SnykAggQueries {

    public static String WONT_FIX_REASON = "wont-fix";
    public static String TEMPORARY_IGNORE_REASON = "temporary-ignore";
    public static String NOT_VULNERABLE_REASON = "not-vulnerable";

    public static QueryField NON_NULL_SEVERITIES = new QueryField("severity",
            STRING, NON_NULL_CHECK, List.of(), null);
    public static QueryField NON_NULL_SCM_URL = new QueryField("scm_url",
            STRING, NON_NULL_CHECK, List.of(), null);
    public static QueryField NON_NULL_SCM_REPO_NAME_PARTIAL = new QueryField("scm_repo_name_partial",
            STRING, NON_NULL_CHECK, List.of(), null);

    private static QueryField constructStringExactMatchField(String fieldName, String fieldValue){
        if(fieldValue == null){
            return null;
        }
        return new QueryField(fieldName, STRING, EXACT_MATCH, Collections.emptyList(), fieldValue);
    }
    private static QueryField[] filterNull(List<QueryField> input){
        List<QueryField> output = input.stream().filter(x -> x!=null).collect(Collectors.toList());
        return output.toArray(new QueryField[output.size()]);
    }

    public static QueryGroup getVulnsQueryBySeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFields = new ArrayList<>();
        queryFields.add(constructStringExactMatchField("severity",severity));
        queryFields.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFields.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));
        return QueryGroup.and(filterNull(queryFields));
    }

    public static QueryGroup getSuppressedVulnsQuery() {
        return QueryGroup.and(
                new QueryField("isIgnored", STRING, EXACT_MATCH,
                        Collections.emptyList(), "true"));
    }

    public static QueryGroup getQueryForSuppressedWithSeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFields = new ArrayList<>();
        queryFields.add(constructStringExactMatchField("severity",severity));
        queryFields.add(constructStringExactMatchField("isIgnored","true"));
        queryFields.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFields.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));
        return QueryGroup.and(filterNull(queryFields));
    }

    public static QueryGroup getQueryForPatchedWithSeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFields = new ArrayList<>();
        queryFields.add(constructStringExactMatchField("severity",severity));
        queryFields.add(constructStringExactMatchField("isPatched","true"));
        queryFields.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFields.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));
        return QueryGroup.and(filterNull(queryFields));
    }

    public static QueryGroup getQueryForTempWithSeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFields = new ArrayList<>();
        queryFields.add(constructStringExactMatchField("severity",severity));
        queryFields.add(constructStringExactMatchField("isIgnored","true"));
        queryFields.add(new QueryField("ignored", STRING, NON_NULL_CHECK, Collections.emptyList(),null));
        queryFields.add(new QueryField("reasonType", JSONB_ARRAY_FIELD,EXACT_MATCH, Collections.singletonList("ignored"),TEMPORARY_IGNORE_REASON));
        queryFields.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFields.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));
        return QueryGroup.and(filterNull(queryFields));
    }

    public static QueryGroup getQueryForWontFixWithSeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFields = new ArrayList<>();
        queryFields.add(constructStringExactMatchField("severity",severity));
        queryFields.add(constructStringExactMatchField("isIgnored","true"));
        queryFields.add(new QueryField("ignored", STRING, NON_NULL_CHECK, Collections.emptyList(),null));
        queryFields.add(new QueryField("reasonType", JSONB_ARRAY_FIELD,EXACT_MATCH, Collections.singletonList("ignored"),WONT_FIX_REASON));
        queryFields.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFields.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));
        return QueryGroup.and(filterNull(queryFields));
    }

    public static QueryGroup getQueryForNotVulnWithSeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFields = new ArrayList<>();
        queryFields.add(constructStringExactMatchField("severity",severity));
        queryFields.add(constructStringExactMatchField("isIgnored","true"));
        queryFields.add(new QueryField("ignored", STRING, NON_NULL_CHECK, Collections.emptyList(),null));
        queryFields.add(new QueryField("reasonType", JSONB_ARRAY_FIELD,EXACT_MATCH, Collections.singletonList("ignored"),NOT_VULNERABLE_REASON));
        queryFields.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFields.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));
        return QueryGroup.and(filterNull(queryFields));
    }

    public static String generateUniqueString(SnykVulnerability vulnerability) {
        return vulnerability.getId() +
                "_" + vulnerability.getProjectId() +
                "_" + vulnerability.getOrgId() +
                "_" + vulnerability.getType() +
                "_" + vulnerability.getFrom() == null ? StringUtils.EMPTY : String.join("_", vulnerability.getFrom()) +
                "_" + vulnerability.getPackageName() +
                "_" + vulnerability.getPublicationTime() +
                "_" + vulnerability.getDisclosureTime() +
                "_" + vulnerability.getPackageManager();
    }

    public static List<QueryGroup> getQueryForCustomSuppressWithSeverity(String severity, String scmUrl, String scmRepoNamePartial) {
        List<QueryField> queryFieldsCondition1 = new ArrayList<>();
        queryFieldsCondition1.add(constructStringExactMatchField("severity",severity));
        queryFieldsCondition1.add(constructStringExactMatchField("isIgnored","true"));
        queryFieldsCondition1.add(new QueryField("ignored", STRING, NON_NULL_CHECK, Collections.emptyList(),null));
        queryFieldsCondition1.add(new QueryField("reasonType", JSONB_ARRAY_FIELD, NOT_EQUALS, Collections.singletonList("ignored"),WONT_FIX_REASON));
        queryFieldsCondition1.add(new QueryField("reasonType", JSONB_ARRAY_FIELD, NOT_EQUALS, Collections.singletonList("ignored"),TEMPORARY_IGNORE_REASON));
        queryFieldsCondition1.add(new QueryField("reasonType", JSONB_ARRAY_FIELD, NOT_EQUALS, Collections.singletonList("ignored"),NOT_VULNERABLE_REASON));
        queryFieldsCondition1.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFieldsCondition1.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));

        List<QueryField> queryFieldsCondition2 = new ArrayList<>();
        queryFieldsCondition2.add(constructStringExactMatchField("severity",severity));
        queryFieldsCondition2.add(constructStringExactMatchField("isIgnored","true"));
        queryFieldsCondition2.add(new QueryField("ignored", STRING, NON_NULL_CHECK, Collections.emptyList(),null));
        queryFieldsCondition2.add(new QueryField("reasonType", JSONB_ARRAY_FIELD, NULL_CHECK, Collections.singletonList("ignored"),null));
        queryFieldsCondition2.add(constructStringExactMatchField("scm_url",scmUrl));
        queryFieldsCondition2.add(constructStringExactMatchField("scm_repo_name_partial",scmRepoNamePartial));

        return List.of(
                QueryGroup.and(filterNull(queryFieldsCondition1)),
                QueryGroup.and(filterNull(queryFieldsCondition2))
        );
    }
}
