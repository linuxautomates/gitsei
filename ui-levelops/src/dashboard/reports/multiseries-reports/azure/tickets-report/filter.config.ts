import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { EffortInvestmentProfileFilterConfig } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  azureCommonFiltersConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemHygieneFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "dashboard/reports/azure/azure-specific-filter-config.constant";
import { METRIC_OPTIONS } from "dashboard/reports/azure/issues-report/constant";
import {
  StackFilterConfig,
  XAxisLabelFilterConfig
} from "dashboard/reports/azure/issues-report/specific-filter-config.constant";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { aggregationMappingsForMultiTimeSeriesReport } from "dashboard/constants/applications/multiTimeSeries.application";
import { ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";

export const AzureMultiSeriesTicketsReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "ticket", "metric"),
  WorkitemresolvedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  generateAcrossFilterConfig(
    aggregationMappingsForMultiTimeSeriesReport(ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT),
    "workitem_created_at"
  ),
  XAxisLabelFilterConfig,
  IssueManagementSystemFilterConfig,
  WorkitemHygieneFilterConfig,
  WorkitemStoryPointsConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  ShowValueOnBarConfig,
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
