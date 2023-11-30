import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { StageDurationFilterConfig } from "dashboard/report-filters/common/stage-duration-filter.config";
import { CicdJobEndDateFilterConfig } from "dashboard/report-filters/common/cicd-job-end-date-filter.config";
import { TicketResolvedAtFilterConfig } from "dashboard/report-filters/jira/ticket-resolved-at-filter.config";
import { generateIssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import {
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";
import { leadTimeCommonFiltersConfig } from "dashboard/report-filters/jira/lead-time-common-filters.config";

export const IssueLeadTimeTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...leadTimeCommonFiltersConfig,
  generateIssueUpdatedAtFilterConfig("jira_issue_updated_at"),
  generateIssueCreatedAtFilterConfig([], "", "jira_issue_created_at"),
  TicketResolvedAtFilterConfig,
  CicdJobEndDateFilterConfig,
  StageDurationFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  EpicsFilterConfig,
  IssueManagementSystemFilterConfig
];
