import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { COMMON_ACROSS_OPTIONS, jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { stageBounceMetricOptions } from "./constant";
import { AcrossFilterConfig, StackFilterConfig, StageFilterConfig } from "./specific-filter-config.constant";

export const JiraStageBounceReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  LastNSprintsFilterConfig,
  StageFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  StackFilterConfig,
  AcrossFilterConfig,
  generateMetricFilterConfig(stageBounceMetricOptions, "default", "mean", "metric"),
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
