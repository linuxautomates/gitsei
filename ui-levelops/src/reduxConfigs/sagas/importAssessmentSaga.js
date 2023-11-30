import { all, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { Q_IMPORT } from "reduxConfigs/actions/actionTypes";
import { RestQuestionnaire, RestSection } from "classes/RestQuestionnaire";

const restapiState = state => state.restapiReducer;

export function* importAssessmentEffectSage(action) {
  const URI = "questionnaires";
  const METHOD = "import";
  let error = false;

  try {
    yield put(actionTypes.restapiLoading(true, URI, METHOD, action.id));

    const { sections, name, kbs } = action.data;
    yield all(
      sections.map((section, index) => {
        const risk_enabled = !!section.questions.filter(
          question => !!question.options.filter(option => !!option.score).length
        ).length;
        return put(
          actionTypes.sectionsCreate(
            new RestSection({ ...section, risk_enabled }),
            `${index}_${action.id}`,
            `SECTION_CREATE_ACTION_${index}_${action.id}`
          )
        );
      })
    );
    yield all(sections.map((section, index) => take(`SECTION_CREATE_ACTION_${index}_${action.id}`)));

    let tag_ids = [];
    let kb_ids = [];
    if (kbs?.[0]) {
      yield put(
        actionTypes.KBsGetOrCreate(
          kbs.map((v, index) => {
            return { value: v, name: `KB-${name}-${index + 1}` };
          }),
          `KB-${name}`
        )
      ); // get or create all the Kbs

      yield take(`KB-${name}`); // wait for them to set in store

      const newState = yield select(restapiState);
      kbs.forEach(kb => {
        const getKbId = newState?.bestpractices?.create[`${KB_CREATE_ACTION}_${kb}`]?.data?.id;
        if (getKbId) {
          kb_ids.push(getKbId);
        }
      });
    }
    const state = yield select(restapiState);
    const newlyCreatedSections = sections.map((section, index) => state.sections.create[`${index}_${action.id}`].data);

    const template = new RestQuestionnaire({ name, tag_ids, kb_ids, sections: newlyCreatedSections });
    const TEMPLATE_CREATED_ACTION = `TEMPLATE_CREATED_ACTION_${action.id}`;
    yield put(actionTypes.qsCreate(template, TEMPLATE_CREATED_ACTION, action.id));
    yield take(TEMPLATE_CREATED_ACTION);
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, action.id));
    yield put(actionTypes.restapiError(error, URI, METHOD, action.id));
  }
}

export function* importAssessmentWatcherSaga() {
  yield takeLatest([Q_IMPORT], importAssessmentEffectSage);
}

export const KB_CREATE_ACTION = "KB_CREATE_ACTION";
