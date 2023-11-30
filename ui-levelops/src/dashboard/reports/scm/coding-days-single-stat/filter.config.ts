import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { CommittedInFilterConfig } from "../scm-specific-filter-config.constant";
import { METRIC_OPTIONS } from "./constant";

export const CodingDaysSingleStatReportFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "average_coding_day"),
  CommittedInFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
