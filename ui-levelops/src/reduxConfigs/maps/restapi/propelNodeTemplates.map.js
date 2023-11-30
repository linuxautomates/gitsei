import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPropelNodeTemplatesToProps = dispatch => {
  return {
    propelNodeTemplatesGet: (id, complete = null) => dispatch(actionTypes.propelNodeTemplatesGet(id, complete)),
    propelNodeTemplatesList: (filters, complete = null) =>
      dispatch(actionTypes.propelNodeTemplatesList(filters, complete))
  };
};
