import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { FIELD_KEY_FOR_FILTERS } from "dashboard/constants/filter-key.mapping";
import { ReactNode } from "react";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMPrLeadTimeByStageReportType extends BaseSCMReportTypes {
  dataKey: string;
  preview_disabled: boolean;
  drilldownFooter: ReactNode;
  includeContextFilter: boolean;
  [FIELD_KEY_FOR_FILTERS]: Record<string, string>;
  [PREV_REPORT_TRANSFORMER]: (widget: any) => any;
  CSV_DRILLDOWN_TRANSFORMER: any;
  get_velocity_config: boolean;
  drilldownCheckbox?: ReactNode;
  getExcludeWithPartialMatchKey?: any;
}
