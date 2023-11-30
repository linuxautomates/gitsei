import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  azureCommonFiltersConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import {
  LeftYAxisFilterConfig,
  RightYAxisFilterConfig,
  SampleIntervalFilterConfig,
  StackFilterConfig
} from "./specific-filter-config.constant";

export const IssueBacklogTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  SortXAxisFilterConfig,
  StackFilterConfig,
  SampleIntervalFilterConfig,
  LeftYAxisFilterConfig,
  RightYAxisFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
