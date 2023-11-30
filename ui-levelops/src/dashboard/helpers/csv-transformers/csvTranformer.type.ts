export type CSVTransformerParamType = {
  apiData: any[];
  columns: any[];
  jsxHeaders?: { title: string; key: string }[];
  filters: { page_size: number; page: number; [x: string]: any };
};
