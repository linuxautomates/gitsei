import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { WeekDateFormatConfig } from "dashboard/report-filters/jira/week-date-format-config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { CommittedInFilterConfig, FileTypeFilterConfig } from "../scm-specific-filter-config.constant";
import { ACROSS_OPTIONS, METRIC_OPTIONS } from "./constant";
import { DaysCountFilterConfig } from "./specific-filter-config.constant";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";

export const CodingDaysReportFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  ShowValueOnBarConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "avg_coding_day_week"),
  FileTypeFilterConfig,
  CommittedInFilterConfig,
  DaysCountFilterConfig,
  SortXAxisFilterConfig,
  WeekDateFormatConfig,
  MaxRecordsFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
