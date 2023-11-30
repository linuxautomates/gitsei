import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { jenkinsCicdCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-scm-cicd-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  JobEndDateFilterConfig,
  JobStartDateFilterConfig
} from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { jenkinsJobsAggsType } from "./constants";

export const JenkinsJobsCommitsLeadSingleStatReportConfig: LevelOpsFilter[] = [
  ...jenkinsCicdCommonFiltersConfig,
  JobEndDateFilterConfig,
  JobStartDateFilterConfig,
  generateAggregationTypesFilterConfig(jenkinsJobsAggsType),
  generateParameterFilterConfig("JOB RUN PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
