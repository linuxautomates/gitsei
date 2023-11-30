import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPropelRunsToProps = dispatch => {
  return {
    propelRunsGet: (id, complete = null) => dispatch(actionTypes.propelRunsGet(id, complete)),
    propelRunsList: (filters, complete = null) => dispatch(actionTypes.propelRunsList(filters, complete))
  };
};
