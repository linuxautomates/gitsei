import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
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
import { METRIC_OPTIONS } from "./constant";
import { AcrossFilterConfig, StackFilterConfig, StageFilterConfig } from "./specific-filter-config.constant";

export const StageBounceReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemresolvedAtFilterConfig,
  generateStoryPointsFilterConfig("workitem_story_points", "AZURE STORY POINTS"),
  StageFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "mean", "metric"),
  StackFilterConfig,
  AcrossFilterConfig,
  MaxRecordsFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
