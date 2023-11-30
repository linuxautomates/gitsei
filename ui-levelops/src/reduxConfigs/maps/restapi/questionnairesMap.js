import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapQuestionnairesToProps = dispatch => {
  return {
    qsList: filters => dispatch(actionTypes.qsList(filters)),
    qsSearch: filters => dispatch(actionTypes.qsSearch(filters)),
    qsGet: id => dispatch(actionTypes.qsGet(id)),
    qsDelete: id => dispatch(actionTypes.qsDelete(id)),
    qsCreate: (questionnaire, id = "0") => dispatch(actionTypes.qsCreate(questionnaire, id)),
    qsUpdate: (id, questionnaire) => dispatch(actionTypes.qsUpdate(id, questionnaire)),
    qsExport: id => dispatch(actionTypes.qsExport(id)),
    qsImport: (data, id = "0") => dispatch(actionTypes.qsImport(data, id)),
    qsNotify: data => dispatch(actionTypes.qsNotify(data))
  };
};
