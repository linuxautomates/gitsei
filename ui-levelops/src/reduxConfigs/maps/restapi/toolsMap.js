import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapToolsToProps = dispatch => {
  return {
    toolsList: filters => dispatch(actionTypes.toolsList(filters)),
    toolsSearch: filters => dispatch(actionTypes.toolsSearch(filters)),
    toolsGet: id => dispatch(actionTypes.toolsGet(id))
  };
};
