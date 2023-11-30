import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJobConfigCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-config-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JenkinsJobConfigChangeCountsTrendsReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobConfigCommonFiltersConfig,
  SortXAxisFilterConfig,
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
