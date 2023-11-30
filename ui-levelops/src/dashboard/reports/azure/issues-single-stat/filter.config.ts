import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import {
  azureCommonFiltersConfig,
  WorkitemFeaturesFilterConfig,
  WorkitemHygieneFilterConfig,
  WorkitemUserStoryFilterConfig
} from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemStoryPointsConfig
} from "../azure-specific-filter-config.constant";
import { TimeRangeTypeFilterConfig } from "./specific-filter-config.constant";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { METRIC_OPTIONS } from "../issues-report/constant";

export const IssueSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "ticket", "metric"),
  TimeRangeTypeFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemHygieneFilterConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemFeaturesFilterConfig,
  WorkitemUserStoryFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
