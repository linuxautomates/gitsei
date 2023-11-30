import { leadTimePhaseTransformer } from "custom-hooks/helpers";
import { CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { leadTimeByTimeSpentInStagesDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeByTimeSpentInStagesCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeByTimeSpentInStagesCSVTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { LeadTimeByTimeSpentInStagesReportTypes } from "model/report/jira/lead-time-by-time-spent-in-stages/leadTimeByTimeSpentInStages.constant";
import { leadTimeByTimeSpentInStagesActionType } from "reduxConfigs/actions/restapi/lead-time.actions";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { JIRA_REVERSE_FILTER_KEY_MAPPING } from "../constant";
import {
  CUSTOM_FIELD_KEY,
  LEAD_TIME_BY_TIME_SPENT_API_BASED_FILTERS,
  LEAD_TIME_BY_TIME_SPENT_CHART_PROPS,
  LEAD_TIME_BY_TIME_SPENT_DEFAULT_QUERY,
  LEAD_TIME_BY_TIME_SPENT_DESCRIPTION,
  requiredOneFiltersKeys
} from "./constant";
import { LeadTimeByTimeSpentInStagesReportFilterConfig } from "./filters.config";
import { REQUIRED_ONE_FILTER, REQUIRED_ONE_FILTER_KEYS } from "dashboard/constants/filter-key.mapping";
import { handleRequiredForFilters } from "../commonJiraReports.helper";
import {
  getDrillDownType,
  getDrilldownTitleInStage,
  getMetadataFiltersPreviewHelper,
  mapFiltersForWidgetApi
} from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const leadTimeByTimeSpentInStagesReport: {
  lead_time_by_time_spent_in_stages_report: LeadTimeByTimeSpentInStagesReportTypes;
} = {
  lead_time_by_time_spent_in_stages_report: {
    name: "Lead Time by Time Spent in Stages",
    description: LEAD_TIME_BY_TIME_SPENT_DESCRIPTION,
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    default_query: LEAD_TIME_BY_TIME_SPENT_DEFAULT_QUERY,
    chart_props: LEAD_TIME_BY_TIME_SPENT_CHART_PROPS,
    uri: "lead_time_by_time_spent_in_stages_report",
    method: "list",
    drilldown: leadTimeByTimeSpentInStagesDrilldown,
    supported_filters: jiraSupportedFilters,
    FIELD_KEY_FOR_FILTERS: JIRA_REVERSE_FILTER_KEY_MAPPING,
    API_BASED_FILTER: LEAD_TIME_BY_TIME_SPENT_API_BASED_FILTERS,
    IS_FRONTEND_REPORT: true, // will remove this once report is available from BE
    report_filters_config: LeadTimeByTimeSpentInStagesReportFilterConfig,
    METADATA_FILTERS_PREVIEW: getMetadataFiltersPreviewHelper,
    CSV_DRILLDOWN_TRANSFORMER: leadTimeByTimeSpentInStagesCsvTransformer,
    STORE_ACTION: leadTimeByTimeSpentInStagesActionType,
    transformFunction: leadTimePhaseTransformer,
    [CUSTOM_FIELD_KEY]: CUSTOM_FIELD_KEY,
    mapFiltersForWidgetApi: mapFiltersForWidgetApi,
    mapFiltersBeforeCall: mapFiltersForWidgetApi,
    getDrilldownTitle: getDrilldownTitleInStage,
    getDrillDownType: getDrillDownType,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string, dashboardTimeRangeKey?: any) => handleRequiredForFilters(config, query, report, dashboardTimeRangeKey),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default leadTimeByTimeSpentInStagesReport;
