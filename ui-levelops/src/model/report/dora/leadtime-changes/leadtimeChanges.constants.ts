import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { WidgetActionFilterType } from "model/filters/levelopsFilters";
import { BaseDoraReportTypes } from "../baseDORAreport.constants";
import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";

export interface DoraLeadTimeChangesReportType extends BaseDoraReportTypes {
  isAdvancedFilterSetting?: boolean;
  [WIDGET_MIN_HEIGHT]: string | number;
  widgetActionSelectFilter: WidgetActionFilterType;
  getDoraProfileIntegrationType: (param: any) => string;
  mapFiltersBeforeCall: (filter: any) => any;
  drilldownFooter?: (param: any) => React.FC | boolean;
  drilldownCheckbox?: (params: any) => React.FC<any> | boolean;
  getDrilldownTitle?: (params?: any) => string;
  getDrillDownType?: () => string;
  [STORE_ACTION]: Function;
  drilldownCheckBoxhandler?: any;
  getDoraProfileIntegrationId: (param: any) => any;
  getDoraSingleStateValue: (param: any) => any;
}
