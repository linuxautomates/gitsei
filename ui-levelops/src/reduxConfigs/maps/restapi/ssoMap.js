import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapSSOToProps = dispatch => {
  return {
    samlssoGet: id => dispatch(actionTypes.samlssoGet(id)),
    samlssoDelete: id => dispatch(actionTypes.samlssoDelete(id)),
    samlssoUpdate: (id, samlsso) => dispatch(actionTypes.samlssoUpdate(id, samlsso))
  };
};
