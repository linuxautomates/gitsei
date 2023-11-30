import { ChartType } from "../../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../../helpers/helper";
import { FE_BASED_FILTERS, REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { issueManagementSupportedFilters } from "../../../constants/supported-filters.constant";
import { azureDrilldown } from "../../../constants/drilldown.constants";

import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS,
  AZURE_PARTIAL_FILTER_KEY_MAPPING
} from "../constant";
import { ACROSS_OPTIONS, DEFAULT_ACROSS, REPORT_NAME, STORY_POINT_URI, URI } from "./constant";
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
import { azureBAProgressReport } from "reduxConfigs/actions/restapi/azureBAProgressActions";
import { azureCommonFilterTransformFunc } from "../helpers/commonFilterTransform.helper";
import { mapFiltersForWidgetApiIssueProgressReport, mapFiltersForWidgetApiIssueProgressReportDrilldown } from "dashboard/constants/bussiness-alignment-applications/helper";

const azureIssuesProgressReport: { azure_issues_progress_report: any } = {
  azure_issues_progress_report: {
    name: REPORT_NAME,
    across: ACROSS_OPTIONS.map((item: { label: string; value: string }) => item.value),
    application: ADO_APPLICATION,
    chart_type: ChartType?.JIRA_PROGRESS_CHART,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: DEFAULT_ACROSS,
    uri: URI,
    method: "list",
    filters: {},
    category: "effort_investment",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    widget_filter_transform: azureCommonFilterTransformFunc,
    [PARTIAL_FILTER_MAPPING_KEY]: AZURE_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    [FE_BASED_FILTERS]: { progressReportEIUnit },
    show_max: true,
    [HIDE_REPORT]: true,
    [MAX_RECORDS_OPTIONS_KEY]: jiraProgressMaxRecordOptions,
    [MAX_RECORDS_LABEL]: "Max Records",
    [WIDGET_MIN_HEIGHT]: "350px",
    [STORE_ACTION]: azureBAProgressReport,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SUPPORT_CATEGORY_EPIC_ACROSS_FILTER]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "story_point_report",
    transformFunction: (data: any) => tableTransformer(data),
    [URI_MAPPING]: {
      tickets_report: URI,
      story_point_report: STORY_POINT_URI
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    valuesToFilters: AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS,
    mapFiltersForWidgetApi: mapFiltersForWidgetApiIssueProgressReport,
    mapFiltersBeforeCall: mapFiltersForWidgetApiIssueProgressReportDrilldown,
  }
};

export default azureIssuesProgressReport;
