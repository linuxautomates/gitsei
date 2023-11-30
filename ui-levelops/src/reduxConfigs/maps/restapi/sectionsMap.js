import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapSectionsToProps = dispatch => {
  return {
    sectionsCreate: (sections, id = "0") => dispatch(actionTypes.sectionsCreate(sections, id)),
    sectionsDelete: id => dispatch(actionTypes.sectionsDelete(id)),
    sectionsUpdate: (id, sections) => dispatch(actionTypes.sectionsUpdate(id, sections)),
    sectionsGet: id => dispatch(actionTypes.sectionsGet(id)),
    sectionsList: filters => dispatch(actionTypes.sectionsList(filters)),
    sectionsSearch: filters => dispatch(actionTypes.sectionsSearch(filters))
  };
};
