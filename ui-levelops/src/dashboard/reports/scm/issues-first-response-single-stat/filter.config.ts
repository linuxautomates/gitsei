import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { githubIssuesStatCommonFiltersConfig } from "dashboard/report-filters/github/github-issues-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { TIME_PERIOD_OPTIONS } from "./constant";
import { LabelFilterConfig } from "./specific-filters-config.constant";

export const IssuesFirstResponseSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...githubIssuesStatCommonFiltersConfig.filter(item => item.beKey != "labels"),
  IssueCreatedAtFilterConfig,
  LabelFilterConfig,
  generateTimePeriodFilterConfig(TIME_PERIOD_OPTIONS, { required: true }),
  { ...AggregationTypesFilterConfig, required: true },
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
