import { pagerdutyServicesTransformer } from "custom-hooks/helpers";
import { pagerdutyFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { AckTrendReportType } from "model/report/pagerduty/ack-trend/ackTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { pagerdutyAckHoursChartProps } from "./constants";
import { DEPRECATED_REPORT } from "../../../constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyAckTrendReport: { pagerduty_ack_trend: AckTrendReportType } = {
  pagerduty_ack_trend: {
    name: "PagerDuty Ack Trend",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_ack_trend",
    method: "list",
    defaultAcross: "incident_priority",
    chart_props: pagerdutyAckHoursChartProps,
    xaxis: false,
    across: ["incident_priority", "incident_urgency", "alert_severity"],
    supported_filters: pagerdutyFilters,
    transformFunction: data => pagerdutyServicesTransformer(data),
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyAckTrendReport;
