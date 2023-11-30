import { testrailsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { testRailsDrilldown } from "dashboard/constants/drilldown.constants";
import { ALLOW_KEY_FOR_STACKS } from "dashboard/constants/filter-key.mapping";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TestsReportType } from "model/report/testRails/tests-report/testsReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { testrailsStackFilters } from "../commonTestRailsReports.constants";
import { testRailsTestsReportChartProps } from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { transformSCMPRsReportPrevQuery } from "dashboard/reports/scm/pr-report/helper";

const testRailsTestsReport: { testrails_tests_report: TestsReportType } = {
  testrails_tests_report: {
    name: "TestRail Test Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    [PREV_REPORT_TRANSFORMER]: data => transformSCMPRsReportPrevQuery(data),
    uri: "testrails_tests_report",
    method: "list",
    filters: {},
    xaxis: true,
    chart_props: testRailsTestsReportChartProps,
    defaultAcross: "milestone",
    stack_filters: testrailsStackFilters,
    supported_filters: testrailsSupportedFilters,
    transformFunction: (data: any) => testrailsTransformer(data),
    drilldown: testRailsDrilldown,
    allow_key_for_stacks: true
  }
};

export default testRailsTestsReport;
