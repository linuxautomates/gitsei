import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../../constants/ou-filters";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { DependencyAnalysisFilterConfig } from "dashboard/report-filters/jira/dependency-analysis-filter.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { HygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";

import {
  JiraDueDateFilterConfig,
  MetricFilterConfig,
  XAxisLabelFilterConfig
} from "dashboard/reports/jira/issues-report/specific-filter-config.constant";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { aggregationMappingsForMultiTimeSeriesReport } from "dashboard/constants/applications/multiTimeSeries.application";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { multiseriesJiraCommonFiltersConfig } from "dashboard/report-filters/multi-series-report/jira/common-filter-config";

export const JiraMultiSeriesIssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...multiseriesJiraCommonFiltersConfig,
  JiraDueDateFilterConfig,
  XAxisLabelFilterConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  MetricFilterConfig,
  generateAcrossFilterConfig(aggregationMappingsForMultiTimeSeriesReport(JiraReports.JIRA_TICKETS_REPORT)),
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  DependencyAnalysisFilterConfig,
  HygieneFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
