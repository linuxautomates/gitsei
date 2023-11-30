import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ActiveWorkUnitFilterConfig } from "dashboard/report-filters/jira/active-work-unit-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { generateEffortInvestmentProfileFilter } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import {
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const EffortAlignmentReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig.filter((item: LevelOpsFilter) => !["statuses", "status_categories"].includes(item.beKey)),
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  {
    ...IssueResolvedAtFilterConfig,
    required: true,
    deleteSupport: false
  },
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  EpicsFilterConfig,
  IssueManagementSystemFilterConfig,
  ActiveWorkUnitFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
