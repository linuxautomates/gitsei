import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPluginresultsToProps = dispatch => {
  return {
    pluginResultsList: (filters, id = "0", complete = null) =>
      dispatch(actionTypes.pluginResultsList(filters, id, complete)),
    pluginResultsDiff: (before, after, complete = null) =>
      dispatch(actionTypes.pluginResultsDiff(before, after, complete)),
    pluginResultsGet: (id, complete = null) => dispatch(actionTypes.pluginResultsGet(id, complete)),
    pluginResultsUpdate: (id, data, complete = null) => dispatch(actionTypes.pluginResultsUpdate(id, data, complete)),
    pluginResultsBulkDelete: ids => dispatch(actionTypes.pluginResultsBulkDelete(ids))
  };
};
