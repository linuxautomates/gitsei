import { SCMPRReportsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { scmCodingDaysDrilldown } from "dashboard/constants/drilldown.constants";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMCodingDaysReportType } from "model/report/scm/scm-coding-days-report/scmCodingDaysReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import {
  CODING_DAYS_CHART_PROPS,
  CODING_DAYS_DEFAULT_QUERY,
  REPORT_FILTERS,
  SCM_CODING_DAYS_DESCRIPTION
} from "./constant";
import { CodingDaysReportFiltersConfig } from "./filter.config";
import { scmCodingDaysOnChartClickHelper, scmCodingDaysWidgetValidationFunction } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmCodingDaysReport: { github_coding_days_report: SCMCodingDaysReportType } = {
  github_coding_days_report: {
    name: "SCM Coding Days Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    description: SCM_CODING_DAYS_DESCRIPTION,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    default_query: CODING_DAYS_DEFAULT_QUERY,
    chart_props: CODING_DAYS_CHART_PROPS,
    filters: REPORT_FILTERS,
    uri: "github_coding_day",
    method: "list",
    API_BASED_FILTER: ["authors", "committers"],
    supported_filters: githubCommitsSupportedFilters,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    drilldown: scmCodingDaysDrilldown,
    onChartClickPayload: scmCodingDaysOnChartClickHelper,
    transformFunction: SCMPRReportsTransformer,
    ALLOWED_WIDGET_DATA_SORTING: true,
    VALUE_SORT_KEY: "commit_days",
    widget_validation_function: scmCodingDaysWidgetValidationFunction,
    report_filters_config: CodingDaysReportFiltersConfig,
    hide_custom_fields: true
  }
};
