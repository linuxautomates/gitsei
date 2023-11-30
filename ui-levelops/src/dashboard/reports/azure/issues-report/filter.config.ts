import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import {
  azureCommonFiltersConfig,
  WorkitemFeaturesFilterConfig,
  WorkitemUserStoryFilterConfig
} from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
import { EffortInvestmentProfileFilterConfig } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemHygieneFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import { METRIC_OPTIONS } from "./constant";
import { getVisualizationOptions } from "dashboard/reports/helper";
import { AcrossFilterConfig, StackFilterConfig, XAxisLabelFilterConfig } from "./specific-filter-config.constant";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";

export const IssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "ticket", "metric"),
  WorkitemresolvedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemFeaturesFilterConfig,
  WorkitemUserStoryFilterConfig,
  StackFilterConfig,
  AcrossFilterConfig,
  XAxisLabelFilterConfig,
  IssueManagementSystemFilterConfig,
  generateVisualizationFilterConfig(getVisualizationOptions, IssueVisualizationTypes.BAR_CHART, false),
  WorkitemHygieneFilterConfig,
  WorkitemStoryPointsConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  {
    ...EffortInvestmentProfileFilterConfig,
    filterMetaData: {
      ...(EffortInvestmentProfileFilterConfig?.filterMetaData || {}),
      allowClearEffortInvestmentProfile: true
    }
  },
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
