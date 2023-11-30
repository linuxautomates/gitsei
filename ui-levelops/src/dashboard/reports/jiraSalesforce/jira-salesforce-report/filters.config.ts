import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { jiraSalesforceCommonFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-salesforce/common-filters.config";

export const JiraSalesforceSupportEscalationReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraSalesforceCommonFiltersConfig,
  { ...IssueCreatedAtFilterConfig, beKey: "created_at", label: "Salesforce Issue Created Date" },
  { ...IssueUpdatedAtFilterConfig, beKey: "updated_at", label: "Salesforce Issue Updated Date" },
  supportManagementSystemFilterConfig
];
