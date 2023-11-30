import { ChartContainerType } from "dashboard/helpers/helper";
import { ResponseTimeReportType } from "model/report/pagerduty/response-time-report/responseTimeReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { pagerdutyResponseTimeReportFilter, pagerdutyResponseTimeReportChartProps } from "./constants";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyResponseTimeReport: { pagerduty_response_report: ResponseTimeReportType } = {
  pagerduty_response_report: {
    name: "PagerDuty Response Times Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.PRODUCTS_AGGS_API_WRAPPER,
    uri: "product_aggs",
    method: "list",
    filters: pagerdutyResponseTimeReportFilter,
    xaxis: false,
    chart_props: pagerdutyResponseTimeReportChartProps,
    [DEPRECATED_REPORT]: true
  }
};
export default pagerdutyResponseTimeReport;
