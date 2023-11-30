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
import { getFilterCommonConfig, getFilterCommonConfigDrillDown } from "../helper";
import { ReportDrilldownColTransFuncType, ReportDrilldownFilterTransFuncType } from "dashboard/dashboard-types/common-types";
import { jenkinsPipelineJobSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { JenkinsPipelineJobTableConfig } from "dashboard/pages/dashboard-tickets/configs/jenkinsTableConfig";

export const JenkinsCICDPipelineJobsDurationReportConfig: LevelOpsFilter[] = [
  JobEndDateFilterConfig,
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

export const getFilterConfigCicdPipelineJobDuration = (params: any) => {
  let { integrationState } = params;
  
  return getFilterCommonConfig(
    {
      integrationState,
      reportConfig: JenkinsCICDPipelineJobsDurationReportConfig,
      columnConfig: jenkinsPipelineCommonFiltersConfig
    }
  );
};

export const getFilterConfigCicdPipelineJobDrillDown: ReportDrilldownFilterTransFuncType = (utilities: any) => {
  
  let { integrationData } = utilities;
  return getFilterCommonConfigDrillDown(
    'filterConfig',
    {
      integrationData,
      filterConfig: jenkinsPipelineJobSupportedFilters
    }
  );
};

export const cicdPipelineJobDrilldownColumnTransformer: ReportDrilldownColTransFuncType = (utilities: any) => {
  
  let { filters } = utilities;
  let { selectedOuIntegration } = filters;
  return getFilterCommonConfigDrillDown(
    'drillDownColumnConfig',
    {
      integrationData: selectedOuIntegration,
      columnConfig: JenkinsPipelineJobTableConfig,
    }
  );
};

