import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { jenkinsJobsCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { AZURE_TIME_PERIOD_OPTIONS } from "dashboard/reports/azure/constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "./specific-filter-config.constant";

export const JenkinsJobsCountSingleStatReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobsCommonFiltersConfig,
  generateTimePeriodFilterConfig(AZURE_TIME_PERIOD_OPTIONS, { required: true }, "JOB START DATE"),
  JobEndDateFilterConfig,
  generateParameterFilterConfig("JOB RUN PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
