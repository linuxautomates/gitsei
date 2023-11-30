import { HygieneReportTrendsType } from "../../../../model/report/azure/hygiene-report-trends/hygiene-report-trends.model";
import { ChartType } from "../../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../../helpers/helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "../../../constants/filter-key.mapping";
import { azureDrilldown } from "../../../constants/drilldown.constants";
import { issueManagementSupportedFilters } from "../../../constants/supported-filters.constant";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  WIDGET_VALIDATION_FUNCTION
} from "../../../constants/filter-name.mapping";
import {
  COMPARE_X_AXIS_TIMESTAMP,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  LABEL_TO_TIMESTAMP,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "../../../constants/applications/names";
import { hygieneWeightValidationHelper } from "../../../helpers/widgetValidation.helper";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  REPORT_LIST_METHOD
} from "../constant";
import { HYGIENE_TREND_URI, CHART_PROPS, DEFAULT_QUERY, REPORT_NAME, URI, FILTERS } from "./constants";
import { OnChartClickPayload } from "./helper";
import { IssueHygieneReportTrendsFiltersConfig } from "./filter.config";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";

const hygieneReportTrends: { azure_hygiene_report_trends: HygieneReportTrendsType } = {
  azure_hygiene_report_trends: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.HYGIENE_AREA_CHART,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: true,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    default_query: DEFAULT_QUERY,
    hygiene_trend_uri: HYGIENE_TREND_URI,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    drilldown: azureDrilldown,
    supported_filters: issueManagementSupportedFilters,
    across: issueManagementSupportedFilters.values,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [LABEL_TO_TIMESTAMP]: false,
    [COMPARE_X_AXIS_TIMESTAMP]: true,
    [INCLUDE_INTERVAL_IN_PAYLOAD]: true,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    onChartClickPayload: OnChartClickPayload,
    [REPORT_FILTERS_CONFIG]: IssueHygieneReportTrendsFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default hygieneReportTrends;
