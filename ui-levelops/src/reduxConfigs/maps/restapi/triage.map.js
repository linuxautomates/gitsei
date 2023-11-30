import * as actionTypes from "reduxConfigs/actions/restapi";

export const maptriageToProps = dispatch => {
  return {
    triageDetailGet: id => dispatch(actionTypes.triageDetailGet(id)),
    triageStagesGet: (id, filter) => dispatch(actionTypes.triageStagesGet(id, filter)),
    triageRuleResultsGet: (id, filter) => dispatch(actionTypes.triageRuleResultsGet(id, filter)),
    triageMatchingJobs: (workItem, id = "0") => dispatch(actionTypes.triageMatchingJobs(workItem, id)),
    fetchTriageResultJobs: (id, filter, complete = null) =>
      dispatch(actionTypes.fetchTriageResultJobs(id, filter, complete))
  };
};
