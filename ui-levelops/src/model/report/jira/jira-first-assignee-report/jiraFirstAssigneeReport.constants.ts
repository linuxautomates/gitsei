import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "dashboard/constants/filter-name.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER } from "dashboard/constants/applications/names";

export type ImplicityIncludeDrilldownFilter = {
  missing_fields: basicMappingType<boolean>;
};

export interface JiraFirstAssigneeReportType extends BaseJiraReportTypes {
  showExtraInfoOnToolTip: Array<string>;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: ImplicityIncludeDrilldownFilter;
  xaxis: boolean;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
}
