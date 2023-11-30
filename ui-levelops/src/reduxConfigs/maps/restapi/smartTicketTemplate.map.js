import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapSmartTicketTemplatesToProps = dispatch => {
  return {
    smartTicketTemplatesCreate: (item, complete = null) =>
      dispatch(actionTypes.smartTicketTemplatesCreate(item, complete)),
    smartTicketTemplatesDelete: (id, complete = null) => dispatch(actionTypes.smartTicketTemplatesDelete(id, complete)),
    smartTicketTemplatesUpdate: (id, item, complete = null) =>
      dispatch(actionTypes.smartTicketTemplatesUpdate(id, item, complete)),
    smartTicketTemplatesGet: (id, complete = null) => dispatch(actionTypes.smartTicketTemplatesGet(id, complete)),
    smartTicketTemplatesList: (filters, complete = null) =>
      dispatch(actionTypes.smartTicketTemplatesList(filters, complete))
  };
};
