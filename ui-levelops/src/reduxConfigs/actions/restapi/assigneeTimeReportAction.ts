import * as actions from "../actionTypes";

const uri = "assignee_time_report";

export const getAssigneeTimeReport = (id = "0", filters: { [x: string]: Array<any> }, complete = null) => ({
  type: actions.ASSIGNEE_TIME_REPORT_FETCH,
  uri,
  method: "list",
  filters,
  id,
  complete
});
