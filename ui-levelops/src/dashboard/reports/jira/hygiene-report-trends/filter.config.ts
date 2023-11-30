import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { JiraIssueHygieneReportFiltersConfig } from "../hygiene-report/filters.config";
import { SampleIntervalFilterConfig, VisualizationFilterConfig } from "./specific-filter-config.constant";
import { WeekDateFormatConfig } from "dashboard/report-filters/jira/week-date-format-config";

export const JiraIssueHygieneReportTrendsFiltersConfig: LevelOpsFilter[] = [
  ...JiraIssueHygieneReportFiltersConfig,
  SampleIntervalFilterConfig,
  WeekDateFormatConfig,
  VisualizationFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
