import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintEndDateFilterConfig } from "dashboard/report-filters/common/sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";
import { ParentStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { SprintFilterConfig } from "../sprint-goal-report/specific-filter-config.constant";
import { METRIC_OPTIONS } from "./constants";
import { MetricViewByFilterConfig, SampleIntervalFilterConfig } from "./specific-filter-config.constant";

export const JiraSprintImpactOfUnestimatedTicketReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  LastNSprintsFilterConfig,
  SprintFilterConfig,
  ParentStoryPointsFilterConfig,
  SampleIntervalFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  MetricViewByFilterConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", undefined, "metric"),
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
