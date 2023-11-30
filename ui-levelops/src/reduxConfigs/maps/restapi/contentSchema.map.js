import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapContentSchemaToProps = dispatch => {
  return {
    contentSchemaList: (filters, complete = null) => dispatch(actionTypes.contentSchemaList(filters, complete))
  };
};
