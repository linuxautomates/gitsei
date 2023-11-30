import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapJiraSalesforceProps = dispatch => {
  return {
    jiraSalesforceGet: (uri, filters, id = "0") => dispatch(actionTypes.jiraSalesforceGet(uri, filters, id))
  };
};
