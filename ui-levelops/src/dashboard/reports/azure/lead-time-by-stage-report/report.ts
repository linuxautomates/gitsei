import { leadTimePhaseTransformer } from "custom-hooks/helpers";
import {
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { azureLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  CSV_DRILLDOWN_TRANSFORMER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { azureLeadTimeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { AzureLeadTimeByStageReportType } from "model/report/azure/lead-time-by-stage-report/lead-time-by-stage-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  LEAD_TIME_EXCLUDE_STAGE_FILTER,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, DATA_KEY, DEFAULT_ACROSS, DEFAULT_QUERY, FILTERS, MIN_WIDTH, REPORT_NAME, URI } from "./constant";
import { LeadTimeByStageReportFiltersConfig } from "./filter.config";
import {
  LEAD_TIME_STAGE_REPORT_DESCRIPTION,
  LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS
} from "dashboard/reports/jira/lead-time-by-stage-report/constants";
import LeadTimeByStageFooter from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import { azureCommonPrevQueryTansformer, getDrilldownCheckBox } from "./helper";

const leadTimeByStageReport: { azure_lead_time_by_stage_report: AzureLeadTimeByStageReportType } = {
  azure_lead_time_by_stage_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: CHART_PROPS,
    dataKey: DATA_KEY,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    defaultAcross: DEFAULT_ACROSS,
    default_query: DEFAULT_QUERY,
    supported_filters: azureLeadTimeSupportedFilters,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [FILTER_WITH_INFO_MAPPING]: [LEAD_TIME_EXCLUDE_STAGE_FILTER],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PREVIEW_DISABLED]: true,
    [WIDGET_MIN_HEIGHT]: MIN_WIDTH,
    drilldown: azureLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: leadTimePhaseTransformer,
    [HIDE_REPORT]: true,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: LeadTimeByStageReportFiltersConfig,
    drilldownFooter: () => LeadTimeByStageFooter,
    drilldownCheckbox: getDrilldownCheckBox,
    drilldownMissingAndOtherRatings: true,
    drilldownTotalColCaseChange: true
  }
};

export default leadTimeByStageReport;
