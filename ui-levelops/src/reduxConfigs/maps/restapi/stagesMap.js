import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapStagesToProps = dispatch => {
  return {
    stagesList: (filters, complete = null) => dispatch(actionTypes.stagesList(filters, complete)),
    stagesUpdate: (id, stage) => dispatch(actionTypes.stagesUpdate(id, stage))
  };
};
