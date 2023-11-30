import * as actions from "../actionTypes";

export const activitylogsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "activitylogs",
  function: "getActivityLogs",
  method: "list"
});

export const activitylogsMap = dispatch => {
  return {
    activitylogs: {
      list: (filters, complete = null) => dispatch(activitylogsList(filters, complete))
    }
  };
};
