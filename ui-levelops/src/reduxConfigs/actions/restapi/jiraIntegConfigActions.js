import * as actions from "../actionTypes";

const uri = "jira_integration_config";

export const integrationConfigsList = (filters, complete, id = "0") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  complete: complete,
  method: "list",
  id: id
});

export const integrationConfigsCreate = data => ({
  type: actions.RESTAPI_READ,
  data: data,
  uri: uri,
  method: "create"
});

export const loadSelectedDashboardIntegrationsConfig = (complete = null) => ({
  type: actions.LOAD_DASHBOARD_INTEGRATIONS_CONFIG,
  complete
});
