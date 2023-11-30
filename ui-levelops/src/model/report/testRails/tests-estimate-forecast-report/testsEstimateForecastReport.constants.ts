import { BaseTestRailsReportTypes } from "../baseTestRailsReport.constant";

export interface TestsEstimateForecastReportType extends BaseTestRailsReportTypes {
  stack_filters?: string[];
}
