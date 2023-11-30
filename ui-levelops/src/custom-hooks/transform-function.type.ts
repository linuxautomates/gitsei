export type TransformFunctionParamType = {
  reportType: string;
  apiData: any[];
  metadata: Record<string, any>;
  records: number; // max-records to show
  filters: Record<string, any>; // mapped filters from getFilters function in widgetApiContainer
  widgetFilters: Record<string, any>; //non mapped widget query
  statUri: string;
  uri: string;
  sortBy: any;
  isMultiTimeSeriesReport: boolean;
  dashMeta: Record<string, any>;
  supportedCustomFields: any[];
  timeFilterKeys: string[];
};

export type TransformFunctionReturnType = Record<"data", any[]>;
