import { BaseTestRailsReportTypes } from "../baseTestRailsReport.constant";

export interface TestsEstimateReportType extends BaseTestRailsReportTypes {
  stack_filters?: string[];
}
