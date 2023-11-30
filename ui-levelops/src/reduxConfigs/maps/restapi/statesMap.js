import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapStatesToProps = dispatch => ({
  statesList: (filter = {}) => dispatch(actionTypes.statesList(filter)),
  statesGet: id => dispatch(actionTypes.statesGet(id)),
  statesDelete: id => dispatch(actionTypes.statesDelete(id)),
  statesCreate: item => dispatch(actionTypes.statesCreate(item)),
  statesUpdate: (id, item) => dispatch(actionTypes.statesUpdate(id, item))
});
