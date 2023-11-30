import TableFiltersPreview from "dashboard/components/dashboard-header/TableFiltersPreview";
import { ChartContainerType, WidgetType } from "dashboard/helpers/helper";
import { TableReportType } from "model/report/levelops/table-report/tableReport.constant";
import { getTableReportData } from "reduxConfigs/actions/reports/table-report/actions";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { TABLE_REPORT_DEFAULT_QUERY } from "./constant";
import { tableReportValidatorFunction } from "./helper";

const TableReport: { table_report: TableReportType } = {
  table_report: {
    name: "Table Report",
    application: "any",
    supported_widget_types: [WidgetType.CONFIGURE_WIDGET],
    widget_height: "12rem",
    default_query: TABLE_REPORT_DEFAULT_QUERY,
    chart_type: ChartType?.LEVELOPS_TABLE_CHART,
    chart_container: ChartContainerType.TABLE_WIDGET_API_WRAPPER,
    widget_validation_function: tableReportValidatorFunction,
    widget_filters_preview_component: TableFiltersPreview,
    STORE_ACTION: getTableReportData,
    IS_FRONTEND_REPORT: true
  }
};

export default TableReport;
