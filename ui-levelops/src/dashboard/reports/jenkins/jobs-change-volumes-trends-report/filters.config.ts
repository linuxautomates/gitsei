import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsCicdCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-scm-cicd-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { SampleIntervalFilterConfig } from "./specific-filter-config.constants";

export const JenkinsChangeVolumeTrendsReportConfig: LevelOpsFilter[] = [
  ...jenkinsCicdCommonFiltersConfig,
  JobEndDateFilterConfig,
  SampleIntervalFilterConfig,
  SortXAxisFilterConfig,
  generateParameterFilterConfig("JOB RUN PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
