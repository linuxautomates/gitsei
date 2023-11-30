import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { MetricFilterConfig, TimeRangeTypeFilterConfig } from "./specific-filter-config.constant";
import { HygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { JiraDueDateFilterConfig } from "../issues-report/specific-filter-config.constant";

export const JiraIssuesSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  TimeRangeTypeFilterConfig,
  MetricFilterConfig,
  JiraDueDateFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  HygieneFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
