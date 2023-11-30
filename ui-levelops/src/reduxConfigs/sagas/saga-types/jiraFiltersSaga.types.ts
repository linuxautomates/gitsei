export type jiraFiltersSagaFilterType = {
  fields: string[];
  filter: {
    integration_ids: string[];
  };
  integration_ids: string[];
};

export type customFiltersOptionsType = { [x: string]: { key: string }[] };

export type categoriesFilterValueInitialDataType = {
  integrationIds: string[];
  custom_hygienes: any[];
  custom_fields: any[];
  records: any[];
  _metadata?: any;
};

export type filterValueApiConfigType = {
  uri: string;
  method: string;
  id: string;
  filters: jiraFiltersSagaFilterType | { filter: jiraFiltersSagaFilterType };
};

// these are the filters that need to be present inside a filter for most api calls
export type basicFiltersType = { integration_ids: string[]; product_id: string };
