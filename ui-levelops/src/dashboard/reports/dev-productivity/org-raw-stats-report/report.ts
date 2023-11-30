import { GET_WIDGET_TITLE_INTERVAL } from "dashboard/constants/applications/names";
import { GET_CUSTOMIZE_TITLE } from "dashboard/constants/filter-key.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { ChartContainerType } from "dashboard/helpers/helper";
import { RawStatsReportType } from "model/report/dev-productivity/raw-stats-report/rawStatsReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { defaultQuery } from "../constants";
import { emptyWidgetPreviewFunc } from "../helper";
import { individualRawStatEmptyWidgetPreviewFunc } from "../individual-raw-stats-report/helper";
import { defaultRawStatColumns, rawStatColumns } from "../rawStatsTable.config";
import { getChartProps, getFilters } from "./helper";
import { widgetEntitlement } from "../widgetEntitlment";

const orgRawStatsReport: { org_raw_stats_report: RawStatsReportType } = {
  org_raw_stats_report: {
    name: "Raw Stats by Collection",
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
    displayColumnSelection: true,
    [WIDGET_MIN_HEIGHT]: "12rem",
    [GET_WIDGET_TITLE_INTERVAL]: true,
    widgetEntitlements: widgetEntitlement
  }
};

export default orgRawStatsReport;
