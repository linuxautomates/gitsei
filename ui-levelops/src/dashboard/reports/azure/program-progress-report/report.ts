import { ChartType } from "../../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../../helpers/helper";
import {
  FE_BASED_FILTERS,
  NO_LONGER_SUPPORTED_FILTER,
  REPORT_FILTERS_CONFIG
} from "../../../constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  IS_FRONTEND_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { issueManagementSupportedFilters } from "../../../constants/supported-filters.constant";
import { azureDrilldown } from "../../../constants/drilldown.constants";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS,
  AZURE_PARTIAL_FILTER_KEY_MAPPING
} from "../constant";
import {
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  PROGRAM_PROGRESS_REPORT_DEFUALT_COLUMNS,
  REPORT_HEADER_INFO,
  REPORT_NAME,
  STORY_POINT_URI,
  URI
} from "./constant";
import { IssuesReportFiltersConfig } from "./filter.config";
import {
  DefaultKeyTypes,
  jiraProgressMaxRecordOptions,
  MAX_RECORDS_LABEL,
  MAX_RECORDS_OPTIONS_KEY,
  STORE_ACTION,
  SUPPORT_CATEGORY_EPIC_ACROSS_FILTER,
  URI_MAPPING
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { progressReportEIUnit } from "dashboard/constants/FE-BASED/ba.FEbased";
import { tableTransformer } from "custom-hooks/helpers";
import { azureBAProgramProgressReport } from "reduxConfigs/actions/restapi/azureBAProgressActions";
import ReportHeaderInfo from "./ReportHeaderInfo";
import {
  AzureProgramProgressTableConfig,
  AzureProgramProgressTableConfigFunc
} from "shared-resources/charts/jira-charts/jira-progress.table-config";
import { AzureProgramProgressReportType } from "model/report/azure/program-progress-report/program-progress-report.model";
import {
  drilldownTransformFunction,
  onChartClickPayload,
  removeHiddenFiltersFromPreview,
  removeNoLongerSupportedFilters
} from "./helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";

const azureProgramProgressReport: { azure_program_progress_report: AzureProgramProgressReportType } = {
  azure_program_progress_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.JIRA_PROGRESS_CHART,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: DEFAULT_ACROSS,
    uri: URI,
    method: "list",
    filters: {},
    default_query: DEFAULT_QUERY,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: AZURE_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: issueManagementSupportedFilters,
    drilldown: {
      ...azureDrilldown,
      drilldownVisibleColumn: ["workitem_id", "summary", "components", "workitem_type", "priority", "story_point"],
      supportExpandRow: false,
      drilldownTransformFunction: drilldownTransformFunction
    },
    [FE_BASED_FILTERS]: { progressReportEIUnit },
    show_max: true,
    [MAX_RECORDS_OPTIONS_KEY]: jiraProgressMaxRecordOptions,
    [MAX_RECORDS_LABEL]: "Max Records",
    [WIDGET_MIN_HEIGHT]: "350px",
    [STORE_ACTION]: azureBAProgramProgressReport,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SUPPORT_CATEGORY_EPIC_ACROSS_FILTER]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "story_point_report",
    transformFunction: (data: any) => tableTransformer(data),
    [URI_MAPPING]: {
      tickets_report: URI,
      story_point_report: STORY_POINT_URI
    },
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    [REPORT_HEADER_INFO]: (params: any) => {
      return ReportHeaderInfo({ ...params });
    },
    [IS_FRONTEND_REPORT]: true,
    displayColumnSelection: true,
    available_columns_func: AzureProgramProgressTableConfigFunc,
    available_columns: AzureProgramProgressTableConfig,
    default_columns: AzureProgramProgressTableConfig.filter(c =>
      PROGRAM_PROGRESS_REPORT_DEFUALT_COLUMNS.includes(c?.dataIndex)
    ),
    onChartClickPayload: onChartClickPayload,
    valuesToFilters: AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS,
    description: "Shows progress for a PI by features.",
    removeHiddenFiltersFromPreview: removeHiddenFiltersFromPreview,
    prev_report_transformer: azureCommonPrevQueryTansformer,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters)
  }
};

export default azureProgramProgressReport;
