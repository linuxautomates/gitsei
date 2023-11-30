import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { generateIssueClosedAtFiltersConfig } from "dashboard/report-filters/common/issue-closed-at-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { githubSCMIssuesTimeAcrossStagesCommonFilters } from "dashboard/report-filters/github/github-issues-time-across-stages-filters.config";
import { getAcrossValue } from "dashboard/reports/jira/issues-report/helper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constant";
import {
  ExcludeStatusFiltersConfig,
  MetricFilterConfig,
  StackByHistoricalStatusFilterConfig
} from "./specific-filter-config.constant";

export const SCMIssuesTimeAcrossStagesFiltersConfig: LevelOpsFilter[] = [
  ...githubSCMIssuesTimeAcrossStagesCommonFilters,
  IssueCreatedAtFilterConfig,
  MetricFilterConfig,
  MaxRecordsFilterConfig,
  ShowValueOnBarConfig,
  ExcludeStatusFiltersConfig,
  generateIssueClosedAtFiltersConfig("Issue Closed In"),
  { ...generateAcrossFilterConfig(ACROSS_OPTIONS), getMappedValue: getAcrossValue },
  StackByHistoricalStatusFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
