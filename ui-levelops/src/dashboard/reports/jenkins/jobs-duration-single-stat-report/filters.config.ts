import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { jenkinsJobsCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { AZURE_TIME_PERIOD_OPTIONS } from "dashboard/reports/azure/constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { jenkinsJobsAggsType } from "./constants";

export const JenkinsJobsDurationSingleStatReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobsCommonFiltersConfig,
  generateTimePeriodFilterConfig(AZURE_TIME_PERIOD_OPTIONS, { required: true }, "JOB START DATE"),
  JobEndDateFilterConfig,
  generateAggregationTypesFilterConfig(jenkinsJobsAggsType),
  generateParameterFilterConfig("JOB RUN PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
