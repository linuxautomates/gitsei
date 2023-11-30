import { leadTimeTrendTransformer } from "custom-hooks/helpers";
import { scmLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import { leadTimeCicdSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMLeadTimeTrendReportType } from "model/report/scm/scm-pr-lead-time-trend-report/scmLeadTimeTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  REPORT_FILTERS,
  SCM_LEAD_TIME_API_BASED_FILTERS,
  SCM_LEAD_TIME_DEFAULT_QUERY,
  SCM_LEAD_TIME_TREND_CHART_PROPS,
  SCM_LEAD_TIME_TREND_FIELD_KEY_MAPPING
} from "./constant";
import { PrLeadTimeTrendReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrLeadTimeTrendReport: { scm_pr_lead_time_trend_report: SCMLeadTimeTrendReportType } = {
  scm_pr_lead_time_trend_report: {
    name: "SCM PR Lead Time Trend Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    filters: REPORT_FILTERS,
    chart_props: SCM_LEAD_TIME_TREND_CHART_PROPS,
    uri: "lead_time_report",
    method: "list",
    default_query: SCM_LEAD_TIME_DEFAULT_QUERY,
    convertTo: "days",
    shouldJsonParseXAxis: () => true,
    drilldown: scmLeadTimeDrilldown,
    transformFunction: leadTimeTrendTransformer,
    API_BASED_FILTER: SCM_LEAD_TIME_API_BASED_FILTERS,
    supported_filters: leadTimeCicdSupportedFilters,
    FIELD_KEY_FOR_FILTERS: SCM_LEAD_TIME_TREND_FIELD_KEY_MAPPING,
    report_filters_config: PrLeadTimeTrendReportFiltersConfig,
    hide_custom_fields: true
  }
};
