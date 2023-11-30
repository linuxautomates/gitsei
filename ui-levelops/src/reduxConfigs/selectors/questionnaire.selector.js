import { createSelector } from "reselect";

const questionnaireState = state => state.restapiReducer.quiz.get || {};

export const getQuestionnairesSelector = createSelector(questionnaireState, questionnaire => {
  if (!Object.keys(questionnaire).length) {
    return {};
  }

  return Object.keys(questionnaire).map(item => ({
    item: questionnaire[item]
  }));
});
