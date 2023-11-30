import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsPipelineCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-pipeline-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { SampleIntervalFilterConfig } from "./specific-filter-config.constants";
import { getFilterCommonConfig } from "../helper";

export const JenkinsCICDPipelineJobsDurationTrendsReportConfig: LevelOpsFilter[] = [
  JobEndDateFilterConfig,
  generateParameterFilterConfig("EXECUTION PARAMETERS"),
  SampleIntervalFilterConfig,
  SortXAxisFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];

export const getFilterConfigCicdPipelineJobDurationTrends = (params: any) => {
  let { integrationState } = params;
  
  return getFilterCommonConfig(
    {
      integrationState,
      reportConfig: JenkinsCICDPipelineJobsDurationTrendsReportConfig,
      columnConfig: jenkinsPipelineCommonFiltersConfig
    }
  );
};
