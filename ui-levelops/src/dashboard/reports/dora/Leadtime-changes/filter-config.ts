import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { DoraAdvanceSettingsButtonConfig } from "dashboard/report-filters/dora/dora-advanced-setting-button";
import { ShowDoraGradingConfig } from "dashboard/report-filters/dora/show-dora-grading";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const LeadTimeChangeReportFiltersConfig: LevelOpsFilter[] = [
  ShowDoraGradingConfig,
  DoraAdvanceSettingsButtonConfig,
  IssueManagementSystemFilterConfig
];
