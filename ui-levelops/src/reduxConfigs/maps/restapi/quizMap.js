import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapQuizToProps = dispatch => {
  return {
    quizGet: id => dispatch(actionTypes.quizGet(id)),
    quizDelete: id => dispatch(actionTypes.quizDelete(id)),
    quizUpdate: (id, quiz) => dispatch(actionTypes.quizUpdate(id, quiz)),
    quizCreate: quiz => dispatch(actionTypes.quizCreate(quiz)),
    quizList: (filters, id) => dispatch(actionTypes.quizList(filters, id)),
    quizFileUpload: (id, file) => dispatch(actionTypes.quizFileUpload(id, file))
  };
};
