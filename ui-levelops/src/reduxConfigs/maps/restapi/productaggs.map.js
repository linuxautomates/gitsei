import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapProductaggsToProps = dispatch => {
  return {
    productAggsList: (filters, id = "0", complete = null) =>
      dispatch(actionTypes.productAggsList(filters, id, complete)),
    productAggsGet: (id, complete = null) => dispatch(actionTypes.productAggsGet(id, complete))
  };
};
