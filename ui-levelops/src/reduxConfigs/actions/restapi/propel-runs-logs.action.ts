import * as actions from "../actionTypes";

const uri: string = "propel_runs_logs";

export const getPropelRunsLogs = (filters: any, id: string = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete,
  uri,
  id,
  method: "list"
});
