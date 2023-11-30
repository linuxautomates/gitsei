import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import {
  GET_CUSTOMIZE_TITLE,
  IS_FRONTEND_REPORT,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB
} from "../../../constants/filter-key.mapping";
import { DEV_PRODUCTIVITY_REPORTS, GET_GRAPH_FILTERS } from "dashboard/constants/applications/names";
import { getDynamicColumns, getFilters, transformData, chartProps } from "./helper";
import { DevProductivityPRActivityReportType } from "model/report/dev-productivity/dev-productivity-pr-activity-report/devProductivityPRActivityReport.constants";
import { getPRActivityReportTitle } from "./PRActivityReportTitle";
import { defaultQuery, filters } from "../constants";

const devPRActivityReport: {
  [DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT]: DevProductivityPRActivityReportType;
} = {
  [DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT]: {
    name: "PR Activity",
    application: "dev_productivity",
    chart_type: ChartType.DEV_PROD_ACTIVE_TABLE_CHART,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT,
    method: "list",
    chart_props: chartProps,
    filters: filters,
    default_query: defaultQuery,
    transformFunction: transformData,
    //getDynamicColumns: getDynamicColumns,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [GET_CUSTOMIZE_TITLE]: getPRActivityReportTitle,
    getDynamicColumns: getDynamicColumns,
    [GET_GRAPH_FILTERS]: getFilters,
    [IS_FRONTEND_REPORT]: true,
    height: "100%"
  }
};

export default devPRActivityReport;
