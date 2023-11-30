import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { jenkinsCodeVolVsDeployementCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-code-volume-vs-deployement-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { METRIC_OPTIONS } from "./constants";
import { IntervalFilterConfig } from "./specific-filter-config.constants";

export const JenkinsCodeVolVsDeployementReportConfig: LevelOpsFilter[] = [
  ...jenkinsCodeVolVsDeployementCommonFiltersConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "line_count"),
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  IntervalFilterConfig,
  ShowValueOnBarConfig,
  JobEndDateFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
