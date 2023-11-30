import * as actions from "../actionTypes";

const statusUri = "ingestion_integration_status";
const logsUri = "ingestion_integration_logs";

export const getIngestionIntegrationStatus = (id: string, complete: string | null = null) => ({
  type: actions.RESTAPI_READ,
  id,
  uri: statusUri,
  method: "get",
  complete: complete
});

export const listIngestionIntegrationLogs = (filter: { id: string; filter: any }, id = "0") => ({
  type: actions.RESTAPI_READ,
  id,
  uri: logsUri,
  method: "list",
  data: filter
});

export const integrationIngestionAction = (id: string) => ({
  type: actions.INGESTION_INTEGRATION,
  id,
  uri: statusUri,
  method: "get"
});

export const integrationMonitoringAction = (id: string) => ({
  type: actions.INTEGRATION_MONITORING_ACTION,
  integration_id: id
});
