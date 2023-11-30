import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapGenericToProps = dispatch => {
  return {
    genericList: (uri, method, filters, complete = null, id = "0") =>
      dispatch(actionTypes.genericList(uri, method, filters, complete, id)),
    genericGet: (uri, id, complete = null) => dispatch(actionTypes.genericGet(uri, id, complete)),
    restApiSelectGenericList: (loadAllData = false, uri, method, filters, complete = null, id) =>
      dispatch(actionTypes.restApiSelectGenericList(loadAllData, uri, method, filters, complete, id))
  };
};
