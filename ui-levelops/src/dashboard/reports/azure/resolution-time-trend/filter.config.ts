import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  ExcludeStatusFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";

export const ResolutionTimeTrendsReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  IssueManagementSystemFilterConfig,
  SortXAxisFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  WorkitemresolvedAtFilterConfig,
  ExcludeStatusFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
