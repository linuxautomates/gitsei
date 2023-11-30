import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { SCMReworkVisualizationTypes } from "dashboard/constants/typeConstants";
import { scmReworkVisualizationOptions } from "dashboard/graph-filters/components/Constants";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
import { commit_branchFilterConfig, githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { WeekDateFormatConfig } from "dashboard/report-filters/jira/week-date-format-config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { CommittedInFilterConfig, FileTypeFilterConfig } from "../scm-specific-filter-config.constant";
import { ACROSS_OPTIONS } from "./constant";
import { LegacyCodeFiltersConfig } from "./specific-filters-config.constant";

export const SCMReworkReportFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  FileTypeFilterConfig,
  CommittedInFilterConfig,
  WeekDateFormatConfig,
  MaxRecordsFilterConfig,
  LegacyCodeFiltersConfig,
  ShowValueOnBarConfig,
  commit_branchFilterConfig,
  generateVisualizationFilterConfig(scmReworkVisualizationOptions, SCMReworkVisualizationTypes.STACKED_BAR_CHART),
  {
    ...generateAcrossFilterConfig(ACROSS_OPTIONS),
    getMappedValue: (data: any) => {
      const { allFilters } = data;
      if (allFilters?.across === "trend" && allFilters?.interval) {
        return `${allFilters?.across}_${allFilters?.interval}`;
      }
      return undefined;
    }
  },
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
