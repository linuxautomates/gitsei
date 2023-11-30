import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapConfigsToProps = dispatch => {
  return {
    configsList: (filters, id = 0, complete = null) => dispatch(actionTypes.configsList(filters, id, complete)),
    configsUpdate: (configs, id = 0, complete = null) => dispatch(actionTypes.configsUpdate(configs, id, complete))
  };
};
