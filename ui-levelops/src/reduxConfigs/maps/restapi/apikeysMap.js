import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapApikeysToProps = dispatch => {
  return {
    apikeysList: filters => dispatch(actionTypes.apikeysList(filters)),
    apikeysDelete: id => dispatch(actionTypes.apikeysDelete(id)),
    apikeysCreate: item => dispatch(actionTypes.apikeysCreate(item))
  };
};
