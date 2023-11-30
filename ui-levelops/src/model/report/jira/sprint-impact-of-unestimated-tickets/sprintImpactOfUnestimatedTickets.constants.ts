import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { DEFAULT_METADATA, IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
export interface SprintImpactOfUnestimatedTicketsReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [FE_BASED_FILTERS]: Record<string, any>;
  [DEFAULT_METADATA]: Record<string, any>;
  [IS_FRONTEND_REPORT]: boolean;
}
