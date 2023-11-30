import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { jenkinsCicdJobCountFiltersConfig, jenkinsJobsCommonFiltersConfig } from "dashboard/report-filters/jenkins/jenkins-job-common-filters.config";
import { generateParameterFilterConfig } from "dashboard/report-filters/jenkins/parameter-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JobEndDateFilterConfig } from "../jobs-count-single-stat-report/specific-filter-config.constant";
import { ACROSS_OPTIONS } from "./constants";
import { StackFilterConfig, TriageRuleFilterConfig } from "./specific-filter-config.constants";
import { getFilterCommonConfig, getFilterCommonConfigDrillDown } from "../helper";
import { ReportDrilldownColTransFuncType, ReportDrilldownFilterTransFuncType } from "dashboard/dashboard-types/common-types";
import { JenkinsGithubJobRunTableConfig } from "dashboard/pages/dashboard-tickets/configs/jenkinsTableConfig";
import { jenkinsGithubJobSupportedFilters } from "dashboard/constants/supported-filters.constant";

export const JenkinsJobsCountReportConfig: LevelOpsFilter[] = [
  JobEndDateFilterConfig,
  StackFilterConfig,
  ShowValueOnBarConfig,
  MaxRecordsFilterConfig,
  TriageRuleFilterConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  generateParameterFilterConfig("EXECUTION PARAMETERS"),
  SortXAxisFilterConfig,
  generateOUFilterConfig({
    jenkins: {
      options: OUFiltersMapping.jenkins
    }
  })
];

export const getFilterConfigCicdJobCount = (params: any) => {
  let { integrationState } = params;
  return getFilterCommonConfig(
    {
      integrationState,
      reportConfig: [...JenkinsJobsCountReportConfig, ...jenkinsCicdJobCountFiltersConfig],
      columnConfig: jenkinsJobsCommonFiltersConfig,
    }
  );
};

export const getFilterConfigCicdJobCountDrillDown: ReportDrilldownFilterTransFuncType = (utilities: any) => {

  let { integrationData } = utilities;
  return getFilterCommonConfigDrillDown(
    'filterConfig',
    {
      integrationData,
      filterConfig: jenkinsGithubJobSupportedFilters
    }
  );
};

export const cicdJobCountDrilldownColumnTransformer: ReportDrilldownColTransFuncType = (utilities: any) => {
  
  let { filters } = utilities;
  let { selectedOuIntegration } = filters;
  return getFilterCommonConfigDrillDown(
    'drillDownColumnConfig',
    {
      integrationData: selectedOuIntegration,
      columnConfig: JenkinsGithubJobRunTableConfig,
    }
  );
};

