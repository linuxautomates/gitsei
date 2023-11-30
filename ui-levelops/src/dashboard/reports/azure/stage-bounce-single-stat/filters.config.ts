import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemresolvedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import { METRIC_OPTIONS } from "../stage-bounce-report/constant";
import { StageFilterConfig } from "../stage-bounce-report/specific-filter-config.constant";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { AZURE_TIME_PERIOD_OPTIONS } from "../constant";

export const StageBounceSingleStatReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemresolvedAtFilterConfig,
  generateStoryPointsFilterConfig("workitem_story_points", "AZURE STORY POINTS"),
  StageFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  generateTimePeriodFilterConfig(AZURE_TIME_PERIOD_OPTIONS, { required: true }),
  { ...AggregationTypesFilterConfig, required: true },
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "mean", "metric"),
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
