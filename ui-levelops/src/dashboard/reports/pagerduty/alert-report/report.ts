import { ChartContainerType } from "dashboard/helpers/helper";
import { AlertReportType } from "model/report/pagerduty/alert-report/alertReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { pagerdutyAlertReportChartProps, pagerdutyAlertReportFilter } from "./constants";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyAlertReport: { pagerduty_alert_report: AlertReportType } = {
  pagerduty_alert_report: {
    name: "PagerDuty Alerts Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.PRODUCTS_AGGS_API_WRAPPER,
    uri: "product_aggs",
    method: "list",
    filters: pagerdutyAlertReportFilter,
    xaxis: false,
    chart_props: pagerdutyAlertReportChartProps,
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyAlertReport;
