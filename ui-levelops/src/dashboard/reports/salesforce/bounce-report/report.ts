import { seriesDataTransformer } from "custom-hooks/helpers";
import { salesforceDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SalesforceBounceReportType } from "model/report/salesforce/salesforce-bounce-report/salesforceBounceReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SalesforceBounceReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceBounceReport: { salesforce_bounce_report: SalesforceBounceReportType } = {
  salesforce_bounce_report: {
    name: "Support Bounce Report",
    application: IntegrationTypes.SALESFORCE,
    defaultAcross: "status",
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces"
    },
    uri: "salesforce_bounce_report",
    method: "list",
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    HIDE_REPORT: true,
    report_filters_config: SalesforceBounceReportFiltersConfig
  }
};

export default salesforceBounceReport;
