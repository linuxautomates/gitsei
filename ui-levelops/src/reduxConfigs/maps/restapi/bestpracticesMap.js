import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapBestpracticesToProps = dispatch => {
  return {
    bpsList: (filters, id = 0, complete = null) => dispatch(actionTypes.bpsList(filters, id, complete)),
    bpsGet: id => dispatch(actionTypes.bpsGet(id)),
    bpsSearch: filters => dispatch(actionTypes.bpsSearch(filters)),
    bpsCreate: (bp, id = 0, complete) => {
      console.log("bpsCreate called");
      return dispatch(actionTypes.bpsCreate(bp, id, complete));
    },
    bpsDelete: id => dispatch(actionTypes.bpsDelete(id)),
    bpsUpdate: (id, bps) => dispatch(actionTypes.bpsUpdate(id, bps)),
    bpsSend: bp => dispatch(actionTypes.bpsSend(bp)),
    bpsFileUpload: (id, file) => dispatch(actionTypes.bpsFileUpload(id, file))
  };
};
