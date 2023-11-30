import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPropelTriggerTemplatesToProps = dispatch => {
  return {
    propelTriggerTemplatesGet: (id, complete = null) => dispatch(actionTypes.propelTriggerTemplatesGet(id, complete)),
    propelTriggerTemplatesList: (filters, complete = null) =>
      dispatch(actionTypes.propelTriggerTemplatesList(filters, complete))
  };
};
