import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintEndDateFilterConfig } from "dashboard/report-filters/common/sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { generateAdditionalDoneStatusFilterConfig } from "dashboard/report-filters/jira/AdditionalDoneStatuses.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { METRIC_OPTIONS } from "./constants";
import { PercentileFilterConfig } from "./specific-filter-config.constant";

export const JiraSprintDistributionRetrospectiveReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  PercentileFilterConfig,
  LastNSprintsFilterConfig,
  EpicsFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  generateAdditionalDoneStatusFilterConfig("distribution_stages"),
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "story_points", "agg_metric"),
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
