import { DEFAULT_METADATA, IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { DEPRECATED_MESSAGE, DEPRECATED_NOT_ALLOWED, SIMPLIFY_VALUE } from "dashboard/constants/applications/names";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseSCMReportTypes, SHOW_SINGLE_STAT_EXTRA_INFO } from "../baseSCMReports.constant";

export interface DeploymentFrequencySingleStat extends BaseSCMReportTypes {
  xaxis: boolean;
  supportPartialStringFilters: boolean;
  supportExcludeFilters: boolean;
  [IS_FRONTEND_REPORT]: boolean;
  [SHOW_SINGLE_STAT_EXTRA_INFO]: any;
  across?: any;
  compareField: string;
  widgetSettingsTimeRangeFilterSchema: Array<basicMappingType<any>>;
  hide_custom_fields: boolean;
  [SIMPLIFY_VALUE]?: boolean;
  [DEFAULT_METADATA]: any;
  [DEPRECATED_NOT_ALLOWED]?: boolean;
  [DEPRECATED_MESSAGE]?: string;
}
