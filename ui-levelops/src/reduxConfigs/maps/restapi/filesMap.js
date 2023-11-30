import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapFilesToProps = dispatch => {
  return {
    filesHead: (id, fileName = "download") => dispatch(actionTypes.filesHead(id, fileName)),
    filesGet: (id, fileName = "download", download = true, filters = {}) =>
      dispatch(actionTypes.filesGet(id, fileName, download, filters)),
    filesDelete: id => dispatch(actionTypes.filesDelete(id))
  };
};
