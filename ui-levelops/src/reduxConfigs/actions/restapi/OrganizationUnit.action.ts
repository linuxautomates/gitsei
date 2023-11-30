import * as actions from "../actionTypes";

const uri: string = "organization_unit_management";

export const OrganizationUnitList = (filters: any, id: string) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri,
  id,
  method: "list"
});

export const OrganizationUnitDelete = (filters: any, id: string) => ({
  type: actions.ORGANIZATION_UNIT_DELETE,
  filters,
  id
});

export const OrganizationUnitGet = (id: string, queryParams: any = {}) => ({
  type: actions.ORGANIZATION_UNIT_GET,
  uri,
  id,
  queryparams: queryParams,
  method: "get"
});

/**  for simply getting organization unit through restapi saga */
export const OrganizationUnitRestGet = (id: string, complete: string | null = null) => ({
  type: actions.RESTAPI_READ,
  uri,
  id,
  method: "get",
  complete
});

export const OrganizationUnitCreate = (id: string, messageFlag: boolean = true) => ({
  type: actions.ORGANIZATION_UNIT_SAVE,
  id,
  messageFlag
});

export const OrganizationUnitUpdate = (id: string, messageFlag: boolean = true) => ({
  type: actions.ORGANIZATION_UNIT_UPDATE,
  id,
  messageFlag
});

export const OrganizationUnitClone = (orgUnitToClone: any, id: string) => ({
  type: actions.ORGANIZATION_UNIT_CLONE,
  orgUnitToClone,
  id
});

export const OrganizationUnitDashboards = (parent_ou_id?: string) => ({
  type: actions.ORGANZATION_UNIT_DASHBOARDS,
  parent_ou_id
});

export const OrgUnitFilterValues = (integration: string, id: string) => ({
  type: actions.ORGANIZATION_UNIT_FILTER_VALUES,
  integration,
  id
});

export const OrgCustomConfigData = (
  integrations: string[],
  fieldListUri: string,
  integrationConfigUri: string,
  fieldListId: string,
  integConfigId: string
) => ({
  type: actions.ORGANIZATION_CUSTOM_FIELDS_SAGA,
  integrationConfigUri,
  fieldListId,
  integConfigId,
  fieldListUri,
  integrations
});

export const OrgUnitUtilities = (id: string) => ({
  type: actions.ORGANIZATION_UNIT_UTILITY_SAGA,
  id
});

export const getOrgUnitSpecificVersion = (orgId: string) => ({
  type: actions.RESTAPI_READ,
  id: orgId,
  method: "get",
  uri: "organization_unit_version_control"
});

export const setNewActiveVersion = (payload: any, id: string) => ({
  type: actions.RESTAPI_READ,
  method: "list",
  uri: "organization_unit_version_control",
  data: payload,
  id: id
});

export const ouProductivityScore = (id: string, filters: any) => ({
  type: actions.RESTAPI_READ,
  method: "list",
  uri: "organization_unit_productivity_score",
  data: filters,
  id: id
});

export const listOrgUnitsForIntegration = (id: string, filters: any, complete = "") => ({
  type: actions.RESTAPI_READ,
  method: "get",
  uri: "org_units_for_integration",
  data: filters,
  id,
  complete
});

export const clearOrgUnitsListForIntegration = () => ({
  type: actions.CLEAR_ORG_UNITS_LIST_FOR_INTEGRATION,
  uri: "org_units_for_integration"
});

export const orgUnitPivotsList = (id: string, filters: any, complete: any = null) => ({
  type: actions.RESTAPI_READ,
  method: "list",
  uri: "pivots_list",
  data: filters,
  id,
  complete
});

export const orgUnitDashboardList = (id: string, filters: any) => ({
  type: actions.RESTAPI_READ,
  method: "list",
  uri: "org_dashboard_list",
  data: filters,
  id
});

export const pivotCreate = (data: any) => ({
  type: actions.RESTAPI_READ,
  method: "create",
  uri: "pivots",
  data
});

export const pivotUpdate = (id: string, filters: any) => ({
  type: actions.RESTAPI_READ,
  method: "update",
  uri: "pivots",
  data: filters,
  id
});

export const listOrgUnitsForIntegrations = (id: string, filters: any, complete = "") => ({
  type: actions.RESTAPI_READ,
  method: "list",
  uri: "org_units_for_integration",
  data: filters,
  id,
  complete
});

export const ouDashboardSetDefault = (ouId: string, defaultDashboardId: string) => ({
  type: actions.SET_OU_DEFAULT_DASHBOARD,
  ouId,
  defaultDashboardId
});

export const getOUOptionsAction = (uri: string, method: string, uuid: string, parent_ou?: any) => ({
  type: actions.GET_OU_OPTIONS,
  uri,
  method,
  uuid,
  parent_ou
});

export const getOUFiltersAction = (uri: string, method: string, uuid: string, data?: any) => ({
  type: actions.GET_OU_FILTERS_TO_DISPLAY,
  data,
  uri,
  method,
  uuid
});

export const udpateSelectedDashboard = (uri: string, method: string, uuid: string, integrations: any) => ({
  type: actions.UPDATE_SELECTED_DASHBOARD_DATA,
  integrations,
  uri,
  method,
  uuid
});
