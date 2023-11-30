import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import { DevProductivityScoreReportType } from "model/report/dev-productivity/dev-productivity-score-report/devProductivityScoreReport.constants";
import { chartPropsDevProductivity, defaultQuery, filters } from "./../constants";
import { WIDGET_MIN_HEIGHT } from "../../../constants/helper";
import { csvTransformerFn, getFiltersCount, getGraphFilters, transformerFn } from "./helper";
import { getDynamicColumns } from "./tableConfig";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  GET_CUSTOMIZE_TITLE,
  IS_FRONTEND_REPORT,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB
} from "../../../constants/filter-key.mapping";
import { get } from "lodash";
import { GET_GRAPH_FILTERS, GET_WIDGET_TITLE_INTERVAL } from "../../../constants/applications/names";
import { GET_WIDGET_CHART_PROPS, WIDGET_FILTER_PREVIEW_COUNT } from "../../../constants/filter-name.mapping";
import { emptyWidgetPreviewFunc, getWidgetTitle } from "../helper";

const devScoreProductivityReport: { dev_productivity_score_report: DevProductivityScoreReportType } = {
  dev_productivity_score_report: {
    name: "Trellis Score Report",
    application: "dev_productivity",
    chart_type: ChartType?.DEV_PROD_TABLE_CHART,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "dev_productivity_score_report",
    method: "list",
    xaxis: false,
    chart_props: chartPropsDevProductivity,
    filters: filters,
    default_query: defaultQuery,
    [WIDGET_MIN_HEIGHT]: "12rem",
    transformFunction: transformerFn,
    getDynamicColumns: getDynamicColumns,
    render_empty_widget_preview_func: emptyWidgetPreviewFunc,
    [CSV_DRILLDOWN_TRANSFORMER]: csvTransformerFn,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [IS_FRONTEND_REPORT]: true,
    [GET_GRAPH_FILTERS]: getGraphFilters,
    [GET_WIDGET_CHART_PROPS]: data => {
      const { filters = {} } = data;
      const interval = get(filters, ["filter", "interval"], "");
      return {
        interval
      };
    },
    [WIDGET_FILTER_PREVIEW_COUNT]: getFiltersCount,
    [GET_WIDGET_TITLE_INTERVAL]: true,
    [GET_CUSTOMIZE_TITLE]: getWidgetTitle
  }
};

export default devScoreProductivityReport;
