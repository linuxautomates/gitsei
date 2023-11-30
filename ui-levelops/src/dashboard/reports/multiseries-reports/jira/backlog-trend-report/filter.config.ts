import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { DependencyAnalysisFilterConfig } from "dashboard/report-filters/jira/dependency-analysis-filter.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import {
  LeftYAxisFilterConfig,
  MinimumAgeFilterConfig,
  RightYAxisFilterConfig,
  SampleIntervalFilterConfig,
  SnapshotTimeRangeFilterConfig,
  StackFilterConfig
} from "dashboard/reports/jira/jira-backlog-trend-report/specific-filter-config.constant";
import { multiseriesJiraCommonFiltersConfig } from "dashboard/report-filters/multi-series-report/jira/common-filter-config";

export const JiraMultiSeriesBacklogTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...multiseriesJiraCommonFiltersConfig,
  SnapshotTimeRangeFilterConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  StackFilterConfig,
  LeftYAxisFilterConfig,
  RightYAxisFilterConfig,
  MinimumAgeFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  DependencyAnalysisFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
