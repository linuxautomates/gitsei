import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { SprintEndDateFilterConfig } from "dashboard/report-filters/common/sprint-end-date-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SprintFilterConfig } from "./specific-filter-config.constant";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";

export const JiraSprintGoalReportFiltersConfig: LevelOpsFilter[] = [
  SprintFilterConfig,
  { ...SprintEndDateFilterConfig, required: false },
  IssueManagementSystemFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
