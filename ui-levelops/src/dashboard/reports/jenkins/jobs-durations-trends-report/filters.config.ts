import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJobsCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { SampleIntervalFilterConfig } from "./specific-filter-config.constants";

export const JenkinsJobsDurationTrendsReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobsCommonFiltersConfig,
  JobEndDateFilterConfig,
  generateParameterFilterConfig("JOB RUN PARAMETERS"),
  SampleIntervalFilterConfig,
  SortXAxisFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
