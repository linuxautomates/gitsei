import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsJobsCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { ACROSS_OPTIONS } from "./constants";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { getFilterCommonConfig } from "../helper";

export const JenkinsCICDScmJobsDurationReportConfig: LevelOpsFilter[] = [
  JobEndDateFilterConfig,
  ShowValueOnBarConfig,
  generateParameterFilterConfig("EXECUTION PARAMETERS"),
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  MaxRecordsFilterConfig,
  SortXAxisFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];

export const getFilterConfigCicdJobDuration = (params: any) => {
  let { integrationState } = params;

  return getFilterCommonConfig(
    {
      integrationState,
      reportConfig: JenkinsCICDScmJobsDurationReportConfig,
      columnConfig: jenkinsJobsCommonFiltersConfig
    }
  );
};