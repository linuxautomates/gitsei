import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapWorkitemsToProps = dispatch => {
  return {
    workItemCreate: item => dispatch(actionTypes.workItemCreate(item)),
    workItemCreateBlank: item => dispatch(actionTypes.workItemCreateBlank(item)),
    workItemDelete: id => dispatch(actionTypes.workItemDelete(id)),
    workItemUdpate: (id, item) => dispatch(actionTypes.workItemUdpate(id, item)),
    workItemGet: (id, complete = null) => dispatch(actionTypes.workItemGet(id, complete)),
    workItemList: (filters, id = "0") => dispatch(actionTypes.workItemList(filters, id)),
    workItemPatch: (id, item) => dispatch(actionTypes.workItemPatch(id, item)),
    workItemUpload: (workitemId, file, id = "0") => dispatch(actionTypes.workItemUpload(id, workitemId, file)),
    workItemBulkDelete: ids => dispatch(actionTypes.workItemBulkDelete(ids))
  };
};
