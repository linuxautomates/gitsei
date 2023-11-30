// integration actions

import * as actions from "../actionTypes";
import { RestIntegrations } from "../../../classes/RestIntegrations";

const uri = "integrations";

export const integrationsList = (filters, complete = null, id = "0") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  complete: complete,
  function: "getIntegrations",
  method: "list",
  id
});

export const loadSelectedDashboardIntegrations = (complete = null) => ({
  type: actions.LOAD_DASHBOARD_INTEGRATIONS,
  complete
});

export const integrationsBulk = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  complete: complete,
  function: "getIntegrations",
  method: "bulk"
});

export const integrationsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getIntegration",
  method: "get",
  validator: RestIntegrations
});

export const integrationsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteIntegration",
  method: "delete"
});

export const integrationsCreate = integration => ({
  type: actions.RESTAPI_WRITE,
  data: integration,
  uri: uri,
  function: "createIntegration",
  method: "create"
});

export const integrationsUpdate = (id, integration) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: integration,
  uri: uri,
  function: "updateIntegration",
  method: "update"
});

export const satelliteIntegrationYAMLDownload = (integration, id, tenant) => ({
  type: actions.SELF_ONBOARDING_INTEGRATION_YAML_DOWNLOAD,
  payload: integration,
  integration_id: id,
  tenant,
});

export const integrationsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  uri,
  method: "bulkDelete",
  id: "0",
  payload: ids
});
