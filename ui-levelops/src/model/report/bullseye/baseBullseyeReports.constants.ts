import { HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { optionType } from "dashboard/dashboard-types/common-types";
import { BaseReportTypes } from "../baseReport.constant";

export interface BaseBullseyeReportTypes extends BaseReportTypes {
  xaxis: boolean;
  dataKey: string;
  appendAcrossOptions: optionType[];
  [HIDE_CUSTOM_FIELDS]?: boolean;
}
