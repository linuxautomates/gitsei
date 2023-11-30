import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJunitCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-junit-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";

export const JenkinsJobRunsTestReportConfig: LevelOpsFilter[] = [
  ...jenkinsJunitCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  MaxRecordsFilterConfig,
  SortXAxisFilterConfig,
  ShowValueOnBarConfig,
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
