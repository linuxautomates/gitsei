import { BaseDoraReportTypes } from "../baseDORAreport.constants";

export interface DoraFailureRateReportType extends BaseDoraReportTypes {
  isAdvancedFilterSetting?: boolean;
  mapFiltersBeforeCall: (filter: any) => any;
  getDoraProfileIntegrationType: (param: any) => string;
  getDrilldownTitle: (param: any) => string;
  onChartClickPayload: (param: any) => any;
  getDoraProfileIntegrationId: (param: any) => any;
  getDoraSingleStateValue: (param: any) => any;
  getDoraProfileIntegrationApplication: (param: any) => any;
}
