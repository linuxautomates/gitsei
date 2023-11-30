import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsPipelineCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-pipeline-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { ACROSS_OPTIONS } from "./constants";

export const JenkinsCICDPipelineJobsCountReportConfig: LevelOpsFilter[] = [
  ...jenkinsPipelineCommonFiltersConfig,
  JobEndDateFilterConfig,
  generateParameterFilterConfig("JENKINS  PARAMETERS"),
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  MaxRecordsFilterConfig,
  SortXAxisFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];
