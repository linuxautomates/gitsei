import { pagerdutyServicesTransformer } from "custom-hooks/helpers";
import { pagerdutyServicesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { AfterHoursReportType } from "model/report/pagerduty/after-hours/afterHourReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { pagerdutyAfterHoursChartProps } from "./constants";
import { DEPRECATED_REPORT } from "../../../constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyAfterHoursReport: { pagerduty_after_hours: AfterHoursReportType } = {
  pagerduty_after_hours: {
    name: "PagerDuty After Hours",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_after_hours",
    method: "list",
    defaultAcross: "incident_priority",
    chart_props: pagerdutyAfterHoursChartProps,
    xaxis: false,
    supported_filters: pagerdutyServicesSupportedFilters,
    transformFunction: data => pagerdutyServicesTransformer(data),
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyAfterHoursReport;
