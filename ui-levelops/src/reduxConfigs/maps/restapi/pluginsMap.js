import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPluginsToProps = dispatch => {
  return {
    pluginsList: (filters, complete = null) => dispatch(actionTypes.pluginsList(filters, complete)),
    pluginsUpload: (id, file, complete = null) => dispatch(actionTypes.pluginsUpload(id, file, complete)),
    pluginsCSVUpload: (file, data, complete = null) => dispatch(actionTypes.pluginsCSVUpload(file, data, complete)),
    pluginsTrigger: (id, data, complete = null) => dispatch(actionTypes.pluginsTrigger(id, data, complete)),
    pluginsGet: (id, complete = null) => dispatch(actionTypes.pluginsGet(id, complete))
  };
};
