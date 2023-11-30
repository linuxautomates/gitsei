export type FilterType = {
  label: string;
  key: string;
  value: string[] | string;
  type: string;
};
export type IntegrationFilterType = {
  type: string;
  name: string;
  filters: FilterType[];
  id: string;
};

export type CombinedInfoDataType = {
  key: string;
  value: string;
  className: string;
  failed?: InfoDataType[];
  total?: InfoDataType[];
};

export type InfoDataType = { key: string; value: string; className: string };
