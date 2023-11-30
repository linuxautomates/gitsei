import { TIME_FILTER_RANGE_CHOICE_MAPPER } from "dashboard/constants/applications/names";
import { BasePraetorianReportTypes } from "../basePraetorianReports.constants";

export interface IssuesReportTypes extends BasePraetorianReportTypes {
  [TIME_FILTER_RANGE_CHOICE_MAPPER]: any;
  stack_filters: string[];
  xaxis: boolean;
  across: string[];
}
