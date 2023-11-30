import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapcTemplatesToProps = dispatch => {
  return {
    cTemplatesCreate: item => dispatch(actionTypes.cTemplatesCreate(item)),
    cTemplatesDelete: id => dispatch(actionTypes.cTemplatesDelete(id)),
    cTemplatesUdpate: (id, item) => dispatch(actionTypes.cTemplatesUdpate(id, item)),
    cTemplatesGet: id => dispatch(actionTypes.cTemplatesGet(id)),
    cTemplatesList: filters => dispatch(actionTypes.cTemplatesList(filters)),
    commsCreate: comms => dispatch(actionTypes.commsCreate(comms))
  };
};
