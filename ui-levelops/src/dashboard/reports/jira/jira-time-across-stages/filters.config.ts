import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import {
  AcrossFilterConfig,
  HideStatusFilterConfig,
  MetricFilterConfig,
  StatusFilterConfig
} from "./specific-filter-config.constant";

export const JiraTimeAcrossStagesReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig.filter((item: LevelOpsFilter) => item.beKey !== "statuses"),
  StatusFilterConfig,
  MetricFilterConfig,
  MaxRecordsFilterConfig,
  { ...IssueResolvedAtFilterConfig, label: "Last Closed Date", required: true, deleteSupport: false },
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  AcrossFilterConfig,
  HideStatusFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  ShowValueOnBarConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
