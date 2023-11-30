import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPropelNodeCategoriesToProps = dispatch => {
  return {
    propelNodeCategoriesGet: (id = "list", complete = null) =>
      dispatch(actionTypes.propelNodeCategoriesGet(id, complete))
  };
};
