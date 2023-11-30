import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapMappingsToProps = dispatch => {
  return {
    mappingsCreate: item => dispatch(actionTypes.mappingsCreate(item)),
    mappingsDelete: id => dispatch(actionTypes.mappingsDelete(id)),
    mappingsUpdate: (id, item) => dispatch(actionTypes.mappingsUdpate(id, item)),
    mappingsGet: id => dispatch(actionTypes.mappingsGet(id)),
    mappingsList: filters => dispatch(actionTypes.mappingsList(filters))
  };
};
