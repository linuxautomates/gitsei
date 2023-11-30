import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";

export const JiraIssuesAssigneeTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  EpicsFilterConfig,
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
