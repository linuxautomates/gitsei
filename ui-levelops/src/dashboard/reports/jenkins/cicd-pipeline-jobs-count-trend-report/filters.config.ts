import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsPipelineCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-pipeline-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { SampleIntervalFilterConfig } from "./specific-filter-config.constants";
import { getFilterCommonConfig, getFilterCommonConfigDrillDown } from "../helper";
import { ReportDrilldownColTransFuncType, ReportDrilldownFilterTransFuncType } from "dashboard/dashboard-types/common-types";
import { jenkinsPipelineJobSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { JenkinsPipelineTableConfig } from "dashboard/pages/dashboard-tickets/configs";

export const JenkinsCICDPipelineJobsCountTrendsReportConfig: LevelOpsFilter[] = [
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

export const getFilterConfigCicdPipelineJobsCountTrends = (params: any) => {
  let { integrationState } = params;

  return getFilterCommonConfig(
    {
      integrationState,
      reportConfig: JenkinsCICDPipelineJobsCountTrendsReportConfig,
      columnConfig: jenkinsPipelineCommonFiltersConfig
    }
  );
};

export const getFilterConfigCicdPipelineJobCountTrendsDrillDown: ReportDrilldownFilterTransFuncType = (utilities: any) => {
  
  let { integrationData } = utilities;
  return getFilterCommonConfigDrillDown(
    'filterConfig',
    {
      integrationData,
      filterConfig: jenkinsPipelineJobSupportedFilters
    }
  );
};

export const cicdPipelineJobCountTrendsDrilldownColumnTransformer: ReportDrilldownColTransFuncType = (utilities: any) => {
  
  let { filters } = utilities;
  let { selectedOuIntegration } = filters;
  return getFilterCommonConfigDrillDown(
    'drillDownColumnConfig',
    {
      integrationData: selectedOuIntegration,
      columnConfig: JenkinsPipelineTableConfig,
    }
  );
};