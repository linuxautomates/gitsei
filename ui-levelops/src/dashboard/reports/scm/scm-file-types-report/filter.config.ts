import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { FileTypeFilterConfig } from "../scm-specific-filter-config.constant";
import { METRIC_OPTIONS } from "./constant";

export const GithubFileTypeReportFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  FileTypeFilterConfig,
  generateIssueCreatedAtFilterConfig([], "Time", "time_range"),
  generateMetricFilterConfig(METRIC_OPTIONS, "multiple", [], "metrics", "Metric", true),
  generateOUFilterConfig({
    github: {
      options: OUFiltersMapping.github
    }
  })
];
