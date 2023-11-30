import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJunitCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-junit-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JenkinsJobRunsTestTrendReportConfig: LevelOpsFilter[] = [
  ...jenkinsJunitCommonFiltersConfig,
  SortXAxisFilterConfig,
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
