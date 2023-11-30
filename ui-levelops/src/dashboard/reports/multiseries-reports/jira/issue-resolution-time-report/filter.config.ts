import { ExcludeStatusFilterConfig } from "dashboard/report-filters/jira/exclude-status-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../../constants/ou-filters";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { aggregationMappingsForMultiTimeSeriesReport } from "dashboard/constants/applications/multiTimeSeries.application";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import {
  MetricFilterConfig,
  StatusFilterConfig
} from "dashboard/reports/jira/resolution-time-report/specific-filter-config.constant";
import { multiseriesJiraCommonFiltersConfig } from "dashboard/report-filters/multi-series-report/jira/common-filter-config";

export const JiraMultiSeriesResolutionTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...multiseriesJiraCommonFiltersConfig.filter((item: LevelOpsFilter) => item.beKey !== "statuses"),
  StatusFilterConfig,
  MetricFilterConfig,
  { ...IssueResolvedAtFilterConfig, label: "Last Closed Date", required: true, deleteSupport: false },
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  generateAcrossFilterConfig(aggregationMappingsForMultiTimeSeriesReport(JiraReports.RESOLUTION_TIME_REPORT)),
  ExcludeStatusFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
