import { leadTimePhaseTransformer } from "custom-hooks/helpers";
import { scmLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import { leadTimeCicdSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType, transformAzureLeadTimeStageReportPrevQuery } from "dashboard/helpers/helper";
import { SCMPrLeadTimeByStageReportType } from "model/report/scm/scm-pr-lead-time-by-stage-report/scmPrLeadTimeByStageReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { REVERSE_SCM_COMMON_FILTER_KEY_MAPPING } from "../constant";
import {
  REPORT_FILTERS,
  SCM_LEAD_TIME_STAGE_API_BASED_FILTERS,
  SCM_LEAD_TIME_STAGE_CHART_PROPS,
  SCM_LEAD_TIME_STAGE_DEFAULT_QUERY
} from "./constant";
import { PrLeadTimeByStageReportFiltersConfig } from "./filter.config";
import { getDrilldownCheckBox, getDrilldownFooter, getExcludeWithPartialMatchKey } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrLeadTimeByStageReport: { scm_pr_lead_time_by_stage_report: SCMPrLeadTimeByStageReportType } = {
  scm_pr_lead_time_by_stage_report: {
    name: "SCM PR Lead Time by Stage Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "velocity",
    filters: REPORT_FILTERS,
    default_query: SCM_LEAD_TIME_STAGE_DEFAULT_QUERY,
    chart_props: SCM_LEAD_TIME_STAGE_CHART_PROPS,
    dataKey: "duration",
    uri: "lead_time_report",
    method: "list",
    preview_disabled: true,
    drilldown: scmLeadTimeDrilldown,
    drilldownFooter: getDrilldownFooter,
    includeContextFilter: true,
    supported_filters: leadTimeCicdSupportedFilters,
    prev_report_transformer: transformAzureLeadTimeStageReportPrevQuery,
    CSV_DRILLDOWN_TRANSFORMER: leadTimeCsvTransformer,
    transformFunction: leadTimePhaseTransformer,
    FIELD_KEY_FOR_FILTERS: REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
    API_BASED_FILTER: SCM_LEAD_TIME_STAGE_API_BASED_FILTERS,
    report_filters_config: PrLeadTimeByStageReportFiltersConfig,
    hide_custom_fields: true,
    get_velocity_config: true,
    drilldownCheckbox: getDrilldownCheckBox,
    getExcludeWithPartialMatchKey: getExcludeWithPartialMatchKey,
  }
};
