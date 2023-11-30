import { DEPRECATED_MESSAGE, DEPRECATED_NOT_ALLOWED } from "dashboard/constants/applications/names";
import { IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseSCMReportTypes, SHOW_SINGLE_STAT_EXTRA_INFO } from "../baseSCMReports.constant";

export interface LeadTimeForChangesSingleStatType extends BaseSCMReportTypes {
  xaxis: boolean;
  supportPartialStringFilters?: boolean;
  supportExcludeFilters?: boolean;
  [IS_FRONTEND_REPORT]: boolean;
  [SHOW_SINGLE_STAT_EXTRA_INFO]: any;
  across?: any;
  compareField: string;
  widgetSettingsTimeRangeFilterSchema?: Array<basicMappingType<any>>;
  hide_custom_fields: boolean;
  [DEPRECATED_NOT_ALLOWED]?: boolean;
  [DEPRECATED_MESSAGE]?: string;
}
