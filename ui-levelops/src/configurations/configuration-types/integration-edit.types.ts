export type integrationCustomFieldType = {
  key: string;
  label: string;
  delimiter: string;
  id: string;
};

export type integrationMappingType = {
  uuid: string;
  type: string;
  header: string;
  data: integrationCustomFieldType[];
  noMapping: string;
  paddingTop?: number;
  header_info?: string;
  singleSelectFields?: boolean;
};

export type IntegrationTransformedCFTypes = {
  field_key: string;
  key?: string;
  field_type?: string;
  metadata?: {
    transformed: string;
  };
};
