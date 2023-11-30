import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { EffortInvestmentProfileFilterConfig } from "dashboard/report-filters/jira/effort-investment-profile.filter";
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
import { AcrossFilterConfig, MetricFilterConfig } from "./specific-filter-config.constant";

export const ResolutionTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  MetricFilterConfig,
  { ...WorkitemresolvedAtFilterConfig, required: true, deleteSupport: false },
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  AcrossFilterConfig,
  ExcludeStatusFilterConfig,
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  {
    ...SortXAxisFilterConfig,
    defaultValue: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
  },
  {
    ...EffortInvestmentProfileFilterConfig,
    filterMetaData:{
      ...(EffortInvestmentProfileFilterConfig?.filterMetaData || {}),
      allowClearEffortInvestmentProfile:true
    }
  },
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
