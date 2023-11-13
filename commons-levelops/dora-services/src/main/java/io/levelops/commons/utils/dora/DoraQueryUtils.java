package io.levelops.commons.utils.dora;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class DoraQueryUtils {

    public static String deploymentFrequencyInitQuery() {
        return "SELECT \n" +
                "  EXTRACT(\n" +
                "    EPOCH \n" +
                "    FROM \n" +
                "      trend_interval\n" +
                "  ) as trend, \n" +
                "  CONCAT(\n" +
                "    (\n" +
                "      date_part('day', trend_interval)\n" +
                "    ), \n" +
                "    '-', \n" +
                "    (\n" +
                "      date_part('month', trend_interval)\n" +
                "    ), \n" +
                "    '-', \n" +
                "    (\n" +
                "      date_part('year', trend_interval)\n" +
                "    )\n" +
                "  ) as interval, \n" +
                "  COUNT(id) as ct \n" +
                "FROM \n" +
                "  (\n" +
                "    SELECT \n" +
                "      distinct(id) as id, \n" +
                "      date_trunc('day', trend_interval) as trend_interval \n" +
                "    FROM \n" +
                "      (";
    }

    public static String deploymentFrequencyEndQuery() {
        return ") y\n" +
                "  ) z \n" +
                "GROUP BY \n" +
                "  trend_interval \n" +
                "ORDER BY \n" +
                "  trend_interval DESC, \n" +
                "  ct DESC NULLS FIRST";
    }

    public static String getTagTableJoinWithCommitTable(String company, List<String> integrationIds) {
        String whereCond = "";
        if(CollectionUtils.isNotEmpty(integrationIds)) {
            whereCond = " AND st.integration_id IN (:integration_ids)";
        }

        return "  LEFT JOIN \n" +
                    "   (SELECT SC.COMMIT_SHA,\n" +
                    "       ANYARRAY_UNIQ(ARRAY_AGG(ST.TAG)) AS TAGS \n" +
                    "   FROM "+company+".SCM_COMMITS SC \n" +
                    "   LEFT JOIN "+company+".SCM_TAGS ST ON SC.COMMIT_SHA = ST.COMMIT_SHA \n" +
                    "           AND SC.INTEGRATION_ID = ST.INTEGRATION_ID \n" +
                    whereCond +
                    "   GROUP BY SC.COMMIT_SHA) AS SCM_COMMIT_TAGS \n" +
                    " ON SCM_COMMITS.COMMIT_SHA = SCM_COMMIT_TAGS.COMMIT_SHA \n";

    }
}
