import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapTagsToProps = dispatch => {
  return {
    tagsGet: id => dispatch(actionTypes.tagsGet(id)),
    tagsDelete: id => dispatch(actionTypes.tagsDelete(id)),
    tagsCreate: (tag, id = "0") => dispatch(actionTypes.tagsCreate(tag, id)),
    tagsList: (filters, id = 0, complete = null) => dispatch(actionTypes.tagsList(filters, id, complete)),
    tagsBulkList: (filters, complete = null) => dispatch(actionTypes.tagsBulkList(filters, complete)),
    tagsSearch: filters => dispatch(actionTypes.tagsSearch(filters)),
    tagsGetOrCreate: (tag_names, id = "0") => dispatch(actionTypes.tagsGetOrCreate(tag_names, id))
  };
};
