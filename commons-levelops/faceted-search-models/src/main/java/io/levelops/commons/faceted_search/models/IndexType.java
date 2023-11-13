package io.levelops.commons.faceted_search.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import static io.levelops.commons.faceted_search.models.RequestUtils.DOESNT_SUPPORT_PARTITIONED_INDEX;
import static io.levelops.commons.faceted_search.models.RequestUtils.SUPPORTS_PARTITIONED_INDEX;

@Getter
public enum IndexType {
    WORK_ITEMS("work_items", SUPPORTS_PARTITIONED_INDEX, "workitem_es_index_template", "index_templates/workitem/workitem_es_index_template.json", TimeUnit.HOURS.toMinutes(4)),
    SCM_COMMITS("scm_commits", DOESNT_SUPPORT_PARTITIONED_INDEX, "scm_commits_es_index_template", "index_templates/scm/scm_commits_es_index_template.json", TimeUnit.HOURS.toMinutes(4)),
    SCM_PRS("scm_prs", DOESNT_SUPPORT_PARTITIONED_INDEX, "scm_prs_es_index_template", "index_templates/scm/scm_prs_es_index_template.json", TimeUnit.HOURS.toMinutes(4));

    private final String indexNamePrefix;
    private final boolean supportsPartitionedIndex;
    private final String esIndexTemplateName;
    private final String esIndexTemplatePath;
    private final Long defaultRefreshTimeInMins;

    IndexType(String indexNamePrefix, boolean supportsPartitionedIndex, String esIndexTemplateName, String esIndexTemplatePath, Long defaultRefreshTimeInMins) {
        this.indexNamePrefix = indexNamePrefix;
        this.supportsPartitionedIndex = supportsPartitionedIndex;
        this.esIndexTemplateName = esIndexTemplateName;
        this.esIndexTemplatePath = esIndexTemplatePath;
        this.defaultRefreshTimeInMins = defaultRefreshTimeInMins;
    }

    @JsonCreator
    @Nullable
    public static IndexType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(IndexType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }


    private void validate(final String company) {
        Validate.notBlank(company, "company cannot be blank");
    }

    public String getCombinedIndexAlias(final String company) {
        validate(company);
        return this.getIndexNamePrefix() + "_" + company + "_" + "combined";
    }

    public String getIndexWildcard(final String company) {
        validate(company);
        if(isSupportsPartitionedIndex()) {
            return this.getIndexNamePrefix() + "_" + company + "_" + "*";
        } else {
            return this.getIndexNamePrefix() + "_" + company;
        }
    }

    public String getPartitionedIndexName(final String company, final Long ingestedAtOrCreationTimeStamp) {
        validate(company);
        if(isSupportsPartitionedIndex()) {
            Validate.notNull(ingestedAtOrCreationTimeStamp, "ingestedAt cannot be null");
        }

        if(isSupportsPartitionedIndex()) {
            return this.getIndexNamePrefix() + "_" + company + "_" + ingestedAtOrCreationTimeStamp;
        } else {
            return this.getIndexNamePrefix() + "_" + company;
        }
    }
}
