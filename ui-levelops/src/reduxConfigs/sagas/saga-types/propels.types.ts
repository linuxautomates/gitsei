export type TriggerFieldType = {
  content_type: string;
  default_value: string;
  description: string;
  display_name: string;
  filters: Array<any>;
  hidden: boolean;
  key: string;
  options: Array<any>;
  dynamic_resource_name?: string;
  required: boolean;
  type: string;
  validation: string;
  value_type: string;
};

export type TriggerType = {
  category: string;
  description: string;
  display_name: string;
  fields: Record<string, TriggerFieldType>;
  trigger_type: string;
  type: string;
  ui_data: Record<string, any>;
};

export enum ProjectToWorkspaceMappingKeys {
  NAME = "display_name",
  DESCRIPTION = "description",
  URI = "dynamic_resource_name"
}
