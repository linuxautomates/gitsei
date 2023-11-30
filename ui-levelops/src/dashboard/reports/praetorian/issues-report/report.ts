import { praetorianIssuesReportTransform } from "custom-hooks/helpers/helper";
import { REPORT_FILTERS_CONFIG, TIME_FILTER_RANGE_CHOICE_MAPPER } from "dashboard/constants/applications/names";
import { praetorianDrilldown } from "dashboard/constants/drilldown.constants";
import { praetorianIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { IssuesReportTypes } from "model/report/praetorian/issues-report/issuesReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { praetorianIssuesReportChartTypes } from "./constants";
import { IssuesReportFiltersConfig } from "./filters.config";

const PraetorianIssuesReport: { praetorian_issues_report: IssuesReportTypes } = {
  praetorian_issues_report: {
    name: "Praetorian Issues Report",
    application: "praetorian",
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "praetorian_issues_aggs",
    method: "list",
    filters: {},
    stack_filters: ["category", "priority", "tag", "project"],
    defaultAcross: "category",
    chart_props: praetorianIssuesReportChartTypes,
    supported_filters: praetorianIssuesSupportedFilters,
    xaxis: true,
    drilldown: praetorianDrilldown,
    across: ["category", "priority", "tag", "project"],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      ingested_at: "praetorian_ingested_at"
    },
    transformFunction: (data: any) => praetorianIssuesReportTransform(data),
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig
  }
};

export default PraetorianIssuesReport;
