import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";

export interface HopsCountStatReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
}
