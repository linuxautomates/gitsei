import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { TimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { jenkinsJobConfigCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-config-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JenkinsJobConfigChangeStatReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobConfigCommonFiltersConfig,
  AggregationTypesFilterConfig,
  TimePeriodFilterConfig,
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
