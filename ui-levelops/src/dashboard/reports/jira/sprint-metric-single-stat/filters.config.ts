import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { SprintEndDateFilterConfig } from "dashboard/report-filters/common/sprint-end-date-filter.config";
import { AdditionalDoneStatusFilterConfig } from "dashboard/report-filters/jira/AdditionalDoneStatuses.config";
import { SprintReportFilterConfig } from "dashboard/report-filters/jira/sprint-report-filter.config";
import { SprintGracePeriodFilterConfig } from "dashboard/report-filters/jira/SprintGracePeriod.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { IdealRangeFilterConfig, MetricFilterConfig } from "./specific-filter-config.constant";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";

export const JiraSprintSingleStatReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  SprintReportFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  LastNSprintsFilterConfig,
  MetricFilterConfig,
  IdealRangeFilterConfig,
  SprintGracePeriodFilterConfig,
  AdditionalDoneStatusFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
