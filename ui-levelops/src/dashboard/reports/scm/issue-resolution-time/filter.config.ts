import { scmIssueHygieneTypes } from "dashboard/constants/hygiene.constants";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { IssueClosedAtFilterConfig } from "dashboard/report-filters/common/issue-closed-at-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubIssuesStatCommonFiltersConfig } from "dashboard/report-filters/github/github-issues-common-filters.config";
import { getAcrossValue } from "dashboard/reports/jira/issues-report/helper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS, METRIC_OPTIONS } from "./constant";

export const IssuesResolutionTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...githubIssuesStatCommonFiltersConfig,
  {
    ...generateAcrossFilterConfig(ACROSS_OPTIONS),
    getMappedValue: getAcrossValue
  },
  generateMetricFilterConfig(
    METRIC_OPTIONS,
    "multiple",
    ["median_resolution_time", "number_of_tickets_closed"],
    "metric"
  ),
  IssueClosedAtFilterConfig,
  generateHygieneFilterConfig(scmIssueHygieneTypes.map((item: string) => ({ label: item, value: item }))),
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    github: {
      options: OUFiltersMapping.github
    }
  })
];
