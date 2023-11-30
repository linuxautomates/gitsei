import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";
import { ReleaseDateInFilterConfig } from "dashboard/report-filters/common/release-date-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JiraReleaseTableReportFilterConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  { ...ReleaseDateInFilterConfig, required: true, deleteSupport: false },
  IssueResolvedAtFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
];
