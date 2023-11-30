import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import {
  azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig
} from "dashboard/reports/azure/azure-specific-filter-config.constant";
import { StackFilterConfig } from "dashboard/reports/azure/backlog-trend-report/specific-filter-config.constant";
import {
  LeftYAxisFilterConfig,
  RightYAxisFilterConfig
} from "dashboard/reports/jira/jira-backlog-trend-report/specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const AzureMultiSeriesBacklogTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  StackFilterConfig,
  LeftYAxisFilterConfig,
  RightYAxisFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
