import { pagerdutyServicesTransformer } from "custom-hooks/helpers";
import { pagerdutyServicesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ReleaseIncidentReportType } from "model/report/pagerduty/release-incidents/releaseIncidentsReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jenkinsSupportedFilters, pagerdutyReleaseIncidentsReportChartProps } from "./constants";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyReleaseIncidentReport: { pagerduty_release_incidents: ReleaseIncidentReportType } = {
  pagerduty_release_incidents: {
    name: "PagerDuty Release Incidents",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_release_incidents",
    method: "list",
    defaultAcross: "incident_priority",
    chart_props: pagerdutyReleaseIncidentsReportChartProps,
    xaxis: true,
    across: ["incident_priority", "incident_urgency", "alert_severity"],
    supported_filters: [pagerdutyServicesSupportedFilters, jenkinsSupportedFilters],
    defaultFilters: {
      cicd_job_ids: []
    },
    transformFunction: data => pagerdutyServicesTransformer(data),
    requiredFilters: ["cicd_job_id"],
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyReleaseIncidentReport;
