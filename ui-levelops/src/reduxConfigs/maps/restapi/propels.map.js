import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPrepelsToProps = dispatch => {
  return {
    prepelsCreate: (item, id = "0") => dispatch(actionTypes.prepelsCreate(item, id)),
    prepelsDelete: id => dispatch(actionTypes.prepelsDelete(id)),
    prepelsUpdate: (id, item) => dispatch(actionTypes.prepelsUpdate(id, item)),
    prepelsGet: id => dispatch(actionTypes.prepelsGet(id)),
    propelsList: (filters, id = "0") => dispatch(actionTypes.propelsList(filters, id)),
    prepelsBulk: (filters, complete = null) => dispatch(actionTypes.prepelsBulk(filters, complete))
  };
};
