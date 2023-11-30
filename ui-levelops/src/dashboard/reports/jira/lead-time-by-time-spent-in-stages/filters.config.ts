import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ReleaseDateInFilterConfig } from "dashboard/report-filters/common/release-date-filter.config";
import { StageDurationFilterConfig } from "dashboard/report-filters/common/stage-duration-filter.config";
import {
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const LeadTimeByTimeSpentInStagesReportFilterConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  IssueResolvedAtFilterConfig,
  ReleaseDateInFilterConfig,
  StageDurationFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  EpicsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
