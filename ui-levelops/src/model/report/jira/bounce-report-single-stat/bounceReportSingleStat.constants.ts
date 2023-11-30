import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { Dict } from "types/dict";

export interface JiraBounceSingleStatReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FE_BASED_FILTERS]: Dict<string, any>;
}
