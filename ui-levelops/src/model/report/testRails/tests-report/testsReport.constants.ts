import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { BaseTestRailsReportTypes } from "../baseTestRailsReport.constant";

export interface TestsReportType extends BaseTestRailsReportTypes {
  stack_filters?: string[];
  allow_key_for_stacks: boolean;
  [PREV_REPORT_TRANSFORMER]?: (data: any) => any;
}
