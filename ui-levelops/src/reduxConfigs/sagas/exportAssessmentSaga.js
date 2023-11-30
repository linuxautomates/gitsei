import { all, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { Q_EXPORT } from "reduxConfigs/actions/actionTypes";

const restapiState = state => state.restapiReducer;

export function* exportAssessmentEffectSage(action) {
  const URI = "questionnaires";
  const METHOD = "export";
  const ID = action.id.toString();
  let error = false;
  try {
    yield put(actionTypes.restapiLoading(true, URI, METHOD, ID));

    // Fetch assessment and wait.
    const QS_LOADED_ACTION = "QS_LOADED_ACTION";
    yield put(actionTypes.qsGet(ID, QS_LOADED_ACTION));
    yield take(QS_LOADED_ACTION);

    const apiState = yield select(restapiState);
    if (apiState.questionnaires.get[ID].error) {
      yield put(actionTypes.restapiError(true, "questionnaires", "export", ID));
    }
    const questionnaire = apiState.questionnaires.get[ID].data;

    // Load sections and wait.
    const SECTIONS_LOADED_ACTION = "SECTIONS_LOADED_ACTION";
    yield all(
      questionnaire.sections.map((section, index) =>
        put(actionTypes.sectionsGet(section, `${SECTIONS_LOADED_ACTION}${index}`))
      )
    );

    yield all(questionnaire.sections.map((section, index) => take(`${SECTIONS_LOADED_ACTION}${index}`)));

    let qTags = [];
    let qKBS = [];

    if (questionnaire.kb_ids && questionnaire.kb_ids.length > 0) {
      const KBS_LOADED_ACTIONS = "KBS_LOADED_ACTIONS";
      yield put(actionTypes.bpsList({}, "0", KBS_LOADED_ACTIONS));

      yield take(KBS_LOADED_ACTIONS);
      const newState = yield select(restapiState);
      const kbs = newState.bestpractices.list["0"].data.records;
      kbs.forEach(kb => {
        if (questionnaire.kb_ids.includes(kb.id)) {
          qKBS.push(kb);
        }
      });
    }

    if (questionnaire.tag_ids && questionnaire.tag_ids.length > 0) {
      const TAGS_LOADED_ACTIONS = "TAGS_LOADED_ACTIONS";
      yield put(
        actionTypes.tagsBulkList(
          {
            filter: {
              tag_ids: questionnaire.tag_ids
            }
          },
          TAGS_LOADED_ACTIONS
        )
      );
      yield take(TAGS_LOADED_ACTIONS);
      const newState = yield select(restapiState);
      qTags = newState.tags.bulk["0"].data.records;
    }

    const sections = [];
    const newState = yield select(restapiState);
    questionnaire.sections.map(section => {
      sections.push(newState.sections.get[section].data);
    });
    questionnaire.sections = sections;
    questionnaire.tags = qTags;
    questionnaire.kbs = qKBS;

    yield put(actionTypes.restapiData(questionnaire, URI, METHOD, ID));
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, ID));
    yield put(actionTypes.restapiError(error, URI, METHOD, ID));
  }
}

export function* exportAssessmentWatcherSaga() {
  yield takeLatest([Q_EXPORT], exportAssessmentEffectSage);
}
