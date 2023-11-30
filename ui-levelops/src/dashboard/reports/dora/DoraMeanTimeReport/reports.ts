import { leadTimePhaseTransformer } from "custom-hooks/helpers";
import { GET_GRAPH_FILTERS, PREVIEW_DISABLED, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { DoraLeadTimeForChangeDrilldown } from "dashboard/constants/drilldown.constants";
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  CSV_DRILLDOWN_TRANSFORMER,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  CATEGORY,
  IS_FRONTEND_REPORT
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, issueLeadTimeFilterOptionsMapping } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING, leadTimeExcludeStageFilter } from "dashboard/constants/filterWithInfo.mapping";
import { leadTimeJiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import LeadTimeByStageFooter from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import {
  conditionalUriMethod,
  defaultQuery,
  getChartProps,
  getDoraMeanTimeData,
  getDoraProfileEvent,
  getDoraProfileIntegrationApplication,
  getDoraProfileIntegrationType,
  getDrilldownTitle,
  getFilterConfig,
  getFilters
} from "./helper";
import { LEAD_TIME_VALUES_TO_FILTERS_KEY } from "dashboard/reports/jira/constant";
import {
  LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS,
  LEAD_TIME_BY_STAGE_REPORT_FILTER
} from "dashboard/reports/jira/lead-time-by-stage-report/constants";
import { getDrilldownCheckBox } from "dashboard/reports/jira/lead-time-by-stage-report/helper";
import { onChartClickHandler } from "../helper";
import { DoraMeanTimeToRestoreReportType } from "model/report/dora/MeanTimeToRestore/MeanTimeToRestore.constant";
import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";
import { getMeanTimeWidgetDataAction } from "reduxConfigs/actions/restapi/widgetAPIActions";
import { APIBASEDFILTERS, FIELDKEYSFORFILTERS, LEAD_TIME_MTTR_DESCRIPTION, keysToNeglect } from "../constants";

const doraMeanTimeToRestore: {
  dora_mean_time_to_restore: DoraMeanTimeToRestoreReportType;
} = {
  dora_mean_time_to_restore: {
    name: "Dora Mean Time To Restore",
    application: "any",
    [CATEGORY]: "dora",
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.DORA_API_WRAPPER,
    chart_props: LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS,
    dataKey: "duration",
    uri: "lead_time_report",
    description: LEAD_TIME_MTTR_DESCRIPTION,
    method: "list",
    filters: LEAD_TIME_BY_STAGE_REPORT_FILTER,
    defaultAcross: "velocity",
    default_query: defaultQuery,
    supported_filters: leadTimeJiraSupportedFilters,
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PREVIEW_DISABLED]: true,
    drilldown: {
      ...DoraLeadTimeForChangeDrilldown,
      uri: "mean_time_to_restore_drilldown"
    },
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimePhaseTransformer(data),
    [API_BASED_FILTER]: APIBASEDFILTERS,
    [FIELD_KEY_FOR_FILTERS]: FIELDKEYSFORFILTERS,
    [REPORT_FILTERS_CONFIG]: getFilterConfig,
    valuesToFilters: LEAD_TIME_VALUES_TO_FILTERS_KEY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [GET_GRAPH_FILTERS]: getFilters,
    includeContextFilter: true,
    drilldownFooter: () => LeadTimeByStageFooter,
    drilldownCheckbox: getDrilldownCheckBox,
    drilldownMissingAndOtherRatings: true,
    drilldownTotalColCaseChange: true,
    getDoraProfileIntegrationType: getDoraProfileIntegrationType,
    getDoraProfileEvent: getDoraProfileEvent,
    getChartProps: getChartProps,
    [STORE_ACTION]: getMeanTimeWidgetDataAction,
    onChartClickPayload: onChartClickHandler,
    getDrilldownTitle: getDrilldownTitle,
    conditionalUriMethod: conditionalUriMethod,
    [IS_FRONTEND_REPORT]: true,
    keysToNeglect: keysToNeglect,
    getDoraProfileIntegrationApplication: getDoraProfileIntegrationApplication,
    getDoraLeadTimeMeanTimeData: getDoraMeanTimeData
  }
};
export default doraMeanTimeToRestore;
