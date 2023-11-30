import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJobsCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { TriageRuleFilterConfig } from "../cicd-jobs-count-report/specific-filter-config.constants";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { SampleIntervalFilterConfig } from "./specific-filter-config.constants";

export const JenkinsJobsCountTrendsReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobsCommonFiltersConfig,
  JobEndDateFilterConfig,
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  SampleIntervalFilterConfig,
  SortXAxisFilterConfig,
  TriageRuleFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
