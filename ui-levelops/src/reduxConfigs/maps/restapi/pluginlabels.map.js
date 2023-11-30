import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPluginLabelsToProps = dispatch => {
  return {
    pluginLabelsList: (filters, complete = null) => dispatch(actionTypes.pluginLabelsList(filters, complete)),
    pluginLabelsValues: (id, filters, complete = null) =>
      dispatch(actionTypes.pluginLabelsValues(id, filters, complete))
  };
};
