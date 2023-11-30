import { ChartContainerType } from "dashboard/helpers/helper";
import { IncidentReportType } from "model/report/pagerduty/incident-report/incidentReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { pagerdutyIncidentReportFilter, pagerdutyIncidentReportChartProps } from "./constants";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyIncidentReport: { pagerduty_incident_report: IncidentReportType } = {
  pagerduty_incident_report: {
    name: "PagerDuty Incident Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.PRODUCTS_AGGS_API_WRAPPER,
    uri: "product_aggs",
    method: "list",
    filters: pagerdutyIncidentReportFilter,
    xaxis: false,
    chart_props: pagerdutyIncidentReportChartProps,
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyIncidentReport;
