import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapActivitylogsToProps = dispatch => {
  return {
    activitylogsList: (filters, complete = null) => dispatch(actionTypes.activitylogsList(filters, complete))
  };
};
