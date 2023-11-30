import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { hygieneVisualizationOptions } from "dashboard/graph-filters/components/Constants";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
import { HygieneWeightsFiltersConfig } from "dashboard/report-filters/jira/hygiene-weights-filters-config";
import { SampleIntervalFilterConfig } from "dashboard/reports/jira/hygiene-report-trends/specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig
} from "../azure-specific-filter-config.constant";
import {
  HideScoreFilterConfig,
  IdleLengthFilterConfig,
  PoorDescriptionFilterConfig
} from "../hygiene-report/specific-filter-config.constant";

export const IssueHygieneReportTrendsFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  HygieneWeightsFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  PoorDescriptionFilterConfig,
  IdleLengthFilterConfig,
  HideScoreFilterConfig,
  SampleIntervalFilterConfig,
  generateVisualizationFilterConfig(hygieneVisualizationOptions, "stacked_area", false),
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
