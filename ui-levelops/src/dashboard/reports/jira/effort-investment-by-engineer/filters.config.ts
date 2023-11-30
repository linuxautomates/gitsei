import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { ActiveWorkUnitFilterConfig } from "dashboard/report-filters/jira/active-work-unit-filter.config";
import { StatusOfTheCompletedIssuesConfig } from "dashboard/report-filters/jira/ba-completed-status-filters.config";
import { BAInProgressStatusFilterConfig } from "dashboard/report-filters/jira/ba-inprogress-status-filters.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EffortAttributionFilterConfig } from "dashboard/report-filters/jira/effort-attribution-filter.config";
import { generateEffortInvestmentProfileFilter } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { EffortUnitFilterConfig } from "dashboard/report-filters/jira/effort-unit-filter.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { FilterAssigneeByStatusFilterConfig } from "dashboard/report-filters/jira/filter-assignee-by-status-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import {
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { CommittedInFilterConfig } from "dashboard/reports/scm/scm-specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const EffortInvestmentByEngineerFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig.filter((item: LevelOpsFilter) => !["statuses", "status_categories"].includes(item.beKey)),
  {
    ...IssueResolvedAtFilterConfig,
    required: true,
    deleteSupport: false
  },
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  EpicsFilterConfig,
  IssueManagementSystemFilterConfig,
  EffortUnitFilterConfig,
  EffortAttributionFilterConfig,
  FilterAssigneeByStatusFilterConfig,
  StatusOfTheCompletedIssuesConfig,
  BAInProgressStatusFilterConfig,
  ActiveWorkUnitFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];

export const CommitEffortInvestmentByEngineerFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  {
    ...CommittedInFilterConfig,
    required: true,
    deleteSupport: false
  },
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  IssueManagementSystemFilterConfig,
  EffortUnitFilterConfig,
  ActiveWorkUnitFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
