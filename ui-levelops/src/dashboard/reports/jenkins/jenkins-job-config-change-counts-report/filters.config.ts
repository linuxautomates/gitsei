import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJobConfigCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-config-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";
import { StackFilterConfig } from "./specific-filer-config.constant";

export const JenkinsJobConfigChangeCountsReportConfig: LevelOpsFilter[] = [
  ...jenkinsJobConfigCommonFiltersConfig,
  MaxRecordsFilterConfig,
  SortXAxisFilterConfig,
  StackFilterConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  generateParameterFilterConfig("JENKINS PARAMETERS"),
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
