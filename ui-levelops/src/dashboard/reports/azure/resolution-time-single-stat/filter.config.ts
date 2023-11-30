import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  ExcludeStatusFilterConfig,
  WorkitemStoryPointsConfig
} from "../azure-specific-filter-config.constant";
import { TimeRangeTypeFilterConfig } from "./specific-filter-config.constant";

export const ResolutionTimeSingleStatReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  ExcludeStatusFilterConfig,
  { ...AggregationTypesFilterConfig, required: true },
  TimeRangeTypeFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
