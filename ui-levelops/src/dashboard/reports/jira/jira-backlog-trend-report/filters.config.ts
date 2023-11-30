import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { DependencyAnalysisFilterConfig } from "dashboard/report-filters/jira/dependency-analysis-filter.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
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
} from "./specific-filter-config.constant";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";

export const JiraBacklogTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  SnapshotTimeRangeFilterConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  StackFilterConfig,
  SampleIntervalFilterConfig,
  LeftYAxisFilterConfig,
  RightYAxisFilterConfig,
  MinimumAgeFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  DependencyAnalysisFilterConfig,
  IssueManagementSystemFilterConfig,
  SortXAxisFilterConfig,
  EpicsFilterConfig,
  ShowValueOnBarConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
