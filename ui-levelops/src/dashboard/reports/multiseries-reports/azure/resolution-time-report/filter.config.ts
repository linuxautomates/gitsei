import { aggregationMappingsForMultiTimeSeriesReport } from "dashboard/constants/applications/multiTimeSeries.application";
import { ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { EffortInvestmentProfileFilterConfig } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import {
  AzureCodeAreaFilterConfig,
  azureCommonFiltersConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  ExcludeStatusFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "dashboard/reports/azure/azure-specific-filter-config.constant";
import { MetricFilterConfig } from "dashboard/reports/azure/resolution-time/specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const AzureMultiSeriesResolutionTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  MetricFilterConfig,
  { ...WorkitemresolvedAtFilterConfig, required: true, deleteSupport: false },
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  generateAcrossFilterConfig(
    aggregationMappingsForMultiTimeSeriesReport(ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT),
    "workitem_created_at"
  ),
  ExcludeStatusFilterConfig,
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
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
