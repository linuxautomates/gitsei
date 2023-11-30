import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { WeekDateFormatConfig } from "dashboard/report-filters/jira/week-date-format-config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { DependencyAnalysisFilterConfig } from "dashboard/report-filters/jira/dependency-analysis-filter.config";
import { EffortInvestmentProfileFilterConfig } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { HygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
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
  AcrossFilterConfig,
  JiraDueDateFilterConfig,
  MetricFilterConfig,
  StackFilterConfig,
  VisualizationFilterConfig,
  XAxisLabelFilterConfig
} from "./specific-filter-config.constant";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";

export const JiraIssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  VisualizationFilterConfig,
  ShowValueOnBarConfig,
  JiraDueDateFilterConfig,
  XAxisLabelFilterConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  MetricFilterConfig,
  StackFilterConfig,
  AcrossFilterConfig,
  {
    ...EffortInvestmentProfileFilterConfig,
    filterMetaData:{
      ...(EffortInvestmentProfileFilterConfig?.filterMetaData || {}),
      allowClearEffortInvestmentProfile:true
    }
  },
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  DependencyAnalysisFilterConfig,
  HygieneFilterConfig,
  IssueManagementSystemFilterConfig,
  SortXAxisFilterConfig,
  WeekDateFormatConfig,
  EpicsFilterConfig,
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
