import { OrgUnitType } from "configurations/constants";
import { optionType } from "dashboard/dashboard-types/common-types";
import { ReactNode } from "react";
import { USERROLESUPPER } from "routes/helper/constants";

export type sectionSelectedFilterType = {
  key: string;
  param: string;
  value: any;
  metadata?: any;
  childKeys?: any;
};

export type orgUnitIntegrationFilterType = {
  type: string;
  filters: any;
};

export type orgRestSectionType = {
  id: string;
  integrations: orgUnitIntegrationFilterType[];
  users?: string;
  dynamic_user_definition?: any;
};

export type OrgUnitTypes = OrgUnitType.DELETE | OrgUnitType.USER | OrgUnitType.FILTER;
export type filterFieldConfig = { id: string; filters: sectionSelectedFilterType[] };
export type OrgUnitSectionKeys =
  | "type"
  | "type_id"
  | "integration"
  | "dynamic_user_definition"
  | "users"
  | "csv_users"
  | "user_groups";

export type filterOptionConfig = {
  label: string;
  value: string;
  options: optionType[];
};

export type userGroupType = {
  id: string;
  dynamic_user_definition?: sectionSelectedFilterType[];
  users?: string[];
  csv_users?: any;
};

export type OrgUnitSectionPayloadType = {
  id: string;
  type: string;
  type_id: string;
  integration: filterFieldConfig;
  user_groups: userGroupType[];
};

export type managersConfigType = {
  id: string;
  email: string;
  full_name: string;
  version?: string;
  user_type: USERROLESUPPER;
};

export interface OrgAdminUser {
  id: string;
  first_name: string;
  last_name: string;
  user_type: USERROLESUPPER;
  email: string;
  managed_ou_ref_ids: string[];
}

export type csvUserConfig = {
  email: string;
  name: string;
};

export type defaultUserSectionConfig = {
  dynamic_user_definition?: sectionSelectedFilterType[];
  users?: string[];
  csv_users: any;
};

// type for received collection
export type orgUnitRemoteJSONType = {
  id: string;
  name: string;
  description: string;
  tags: string[];
  managers: managersConfigType[];
  sections: orgRestSectionType[];
  default_section: {
    users?: string;
    dynamic_user_definition?: any;
  };
};

export type orgUnitJSONType = {
  id?: string;
  ou_id?: string;
  name?: string;
  version?: string;
  path?: string;
  description?: string;
  managers?: managersConfigType[];
  tags?: string[];
  sections?: OrgUnitSectionPayloadType[];
  default_section?: defaultUserSectionConfig;
  ou_category_id?: string;
  dashboards?: Array<OUDashboardType>;
  parent_ref_id?: number;
  valid_name?: boolean;
  disabled?: boolean;
  default_dashboard_id?: string;
  children?: Array<orgUnitJSONType>;
  allChilds?: Array<String>;
  key?: string;
  title?: string;
  access_response?: {
    create: boolean;
    edit: boolean;
    delete: boolean;
    view: boolean;
  };
};

export type orgUnitBasicInfoType =
  | "name"
  | "description"
  | "managers"
  | "tags"
  | "sections"
  | "default_section"
  | "version"
  | "ouGroupId"
  | "dashboards"
  | "parentId"
  | "validName"
  | "defaultDashboardId";

export type filterFieldType = "param" | "key" | "value" | "metadata";
export type userSelectionType = "static" | "dynamic";
export type OUIntegrationKeyType = "type" | "selected_filters" | "selected_users";
export type OUUserCreateOptionType = "user_attribute" | "manual" | "import_csv";

export type versionType = {
  version: string;
  timestamp: string;
};

export type OUUserConfigType = {
  id: string;
  user_ids: string[];
};

export type OUDefaultUsersSettingConfigType = {
  application: string;
  options: optionType[];
};

export type ouUserDesignationType = {
  [x: string]: string[];
};

export enum OrgUnitListType {
  FLAT_LIST = "flat_list",
  TREE_LIST = "tree_list"
}

export type PivotType = {
  id: string;
  name: string;
  count_of_ous: number;
  created_at: number;
  description: string;
  group_category: string;
  is_predefined: boolean;
  updated_at: number;
  enabled?: boolean;
  ou_ref_ids?: Array<number>;
};

export type OUEdgeType = {
  ou_id?: string;
  name?: string;
  icon?: ReactNode;
  disabled?: boolean;
};

export type OUEdgeConfig = {
  ou_id?: string;
  name?: string;
  id?: string;
  disabled?: boolean;
  children: Array<OUEdgeType>;
};

export type HeaderPivot = {
  id: string;
  name: string;
  ou_category_id: string;
  ou_id: string;
  ou_group_id: string;
  ou_name: string;
  parent_ref_id: string;
};

export type OUDashboardList = {
  ou_id: string;
  dashboard_order?: number;
  is_default: boolean;
  dashboard_id: number;
  name: string;
};

export type DashboardType = {
  [key: string]: string;
};

export type OUDashboardMetadataType = { display_name: string };
export type OUDashboardType = {
  ou_id: string;
  dashboard_order?: number;
  is_default: boolean;
  dashboard_id: string;
  name: string;
  display_name: string;
};

export type OUDashboardDataType = {
  inherited_dashboards: Array<OUDashboardType>;
  specific_dashboards: Array<OUDashboardType>;
};

export type OUCategoryOptionsType = {
  label: string;
  value: string;
  ouCount: number;
};
