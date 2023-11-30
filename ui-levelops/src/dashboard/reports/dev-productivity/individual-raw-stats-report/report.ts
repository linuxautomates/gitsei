import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { RawStatsReportType } from "model/report/dev-productivity/raw-stats-report/rawStatsReport.constants";
import { getChartProps, getFilters, individualRawStatEmptyWidgetPreviewFunc } from "./helper";
import { defaultQuery } from "../constants";
import { defaultRawStatColumns, rawStatColumns } from "../rawStatsTable.config";
import { rawStatsDrilldown } from "dashboard/constants/drilldown.constants";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { GET_WIDGET_TITLE_INTERVAL } from "dashboard/constants/applications/names";
import { getWidgetTitle } from "../helper";
import { GET_CUSTOMIZE_TITLE } from "dashboard/constants/filter-key.mapping";
import { widgetEntitlement } from "../widgetEntitlment";

const individualRawStatsReport: { individual_raw_stats_report: RawStatsReportType } = {
  individual_raw_stats_report: {
    name: "Individual Raw Stats",
    application: "dev_productivity",
    chart_type: ChartType.FILTERABLE_RAW_STATS_TABLE,
    chart_container: ChartContainerType.DEV_PROD_WRAPPER,
    show_metrics_tab: false,
    show_aggregations_tab: false,
    filters: {},
    getChartProps: getChartProps,
    get_filters: getFilters,
    IS_FRONTEND_REPORT: true,
    render_empty_widget_preview_func: individualRawStatEmptyWidgetPreviewFunc,
    default_query: defaultQuery,
    available_columns: rawStatColumns,
    default_columns: defaultRawStatColumns,
    drilldown: rawStatsDrilldown,
    displayColumnSelection: true,
    [WIDGET_MIN_HEIGHT]: "12rem",
    [GET_WIDGET_TITLE_INTERVAL]: true,
    [GET_CUSTOMIZE_TITLE]: getWidgetTitle,
    widgetEntitlements: widgetEntitlement
  }
};

export default individualRawStatsReport;
