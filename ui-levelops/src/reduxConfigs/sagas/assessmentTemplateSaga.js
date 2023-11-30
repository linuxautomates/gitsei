import { all, put, select, take, takeLatest, call } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { ASSESSMENT_TEMPLATE_POST, ASSESSMENT_TEMPLATE_GET } from "reduxConfigs/actions/actionTypes";
import { RestQuestionnaire } from "classes/RestQuestionnaire";
import { createTagsByNames } from "./getOrCreateTagsSaga";
import { restapiData } from "../actions/restapi";

const restapiState = state => state.restapiReducer;

export function* assessmentTemplateSaveEffectSage(action) {
  const URI = "complete-questionnaires";
  const METHOD = "createOrUpdate";
  let error = false;
  const template = action.data;
  let templateId = "";

  try {
    const { tag_ids } = template.json();
    let tags = [];

    const createTags = tag_ids.filter(tag => tag.includes("create:"));
    tags = tag_ids.filter(tag => !tag.includes("create:"));
    if (createTags.length) {
      yield call(createTagsByNames, createTags);
      const state = yield select(restapiState);
      const newlyCreatedTags = createTags.map(tag => state.tags.create[tag].data.id);
      tags = [...tags, ...newlyCreatedTags];
    }

    const SECTION_CREATE_ACTION = "SECTION_CREATE_ACTION";
    const SECTION_UPDATE_ACTION = "SECTION_UPDATE_ACTION";
    const SECTION_DELETE_ACTION = "SECTION_DELETE_ACTION";

    let sections = [];

    if (template.json().id) {
      const update = template._sections.filter(section => section.id);
      const create = template._sections.filter(section => !section.id);
      const del = template.deletedSectionIds;

      yield all(
        update.map(section =>
          put(actionTypes.sectionsUpdate(section.id, section, `${SECTION_UPDATE_ACTION}_${section.id}`))
        )
      );
      yield all(update.map(section => take(`${SECTION_UPDATE_ACTION}_${section.id}`)));

      yield all(
        create.map((section, index) =>
          put(actionTypes.sectionsCreate(section, index, `${SECTION_CREATE_ACTION}_${index}`))
        )
      );
      yield all(create.map((section, index) => take(`${SECTION_CREATE_ACTION}_${index}`)));

      yield all(
        del.map(sectionId => put(actionTypes.sectionsDelete(sectionId, `${SECTION_DELETE_ACTION}_${sectionId}`)))
      );
      yield all(del.map(sectionId => take(`${SECTION_DELETE_ACTION}_${sectionId}`)));

      const state = yield select(restapiState);
      const updatedSections = update.map(section => ({ id: section.id }));
      const createdSections = create.map((section, index) => state.sections.create[index].data);

      sections = [...updatedSections, ...createdSections];

      const templateData = { ...template.json(), name: template?.json().name?.trim(), sections, tag_ids: tags };
      const TEMPLATE_UPDATE_ACTION = `TEMPLATE_UPDATE_ACTION_${action.id}`;
      yield put(actionTypes.qsUpdate(templateData.id, new RestQuestionnaire(templateData), TEMPLATE_UPDATE_ACTION));
      yield take(TEMPLATE_UPDATE_ACTION);
      templateId = template.json().id;
    } else {
      yield all(
        template._sections.map((section, index) =>
          put(actionTypes.sectionsCreate(section, index, `${SECTION_CREATE_ACTION}_${index}`))
        )
      );
      yield all(template._sections.map((section, index) => take(`${SECTION_CREATE_ACTION}_${index}`)));

      const state = yield select(restapiState);
      sections = template._sections.map((section, index) => state.sections.create[index].data);

      const templateData = { ...template.json(), name: template?.json().name?.trim(), sections, tags_ids: tags };
      const TEMPLATE_CREATED_ACTION = `TEMPLATE_CREATED_ACTION_${action.id}`;
      yield put(actionTypes.qsCreate(new RestQuestionnaire(templateData), TEMPLATE_CREATED_ACTION, action.id));
      yield take(TEMPLATE_CREATED_ACTION);

      const qState = yield select(restapiState);
      templateId = qState.questionnaires.create[0].data.id;
    }
  } catch (e) {
    error = true;
  } finally {
    yield put(restapiData({ id: templateId }, URI, METHOD, 0));
    yield put(actionTypes.restapiLoading(false, URI, METHOD, "0"));
    yield put(actionTypes.restapiError(error, URI, METHOD, "0"));
  }
}

export function* assessmentTemplateGetEffectSage(action) {
  const URI = "complete-questionnaires";
  const METHOD = "get";
  let error = false;

  const templateId = action.data;

  try {
    yield put(actionTypes.qsGet(templateId, `GET_TEMPLATE_${templateId}`));
    yield take(`GET_TEMPLATE_${templateId}`);

    const state = yield select(restapiState);
    const template = state.questionnaires.get[templateId].data;

    const tags = template.tag_ids;
    const kbs = template.kb_ids;
    const sections = template.sections;

    let _tags = [];
    let _kbs = [];
    let _sections = [];

    if (tags.length) {
      const filters = {
        filter: {
          tag_ids: template.tag_ids
        }
      };

      yield put(actionTypes.tagsBulkList(filters, `TEMPLATE_TAGS_${templateId}`));
      yield take(`TEMPLATE_TAGS_${templateId}`);

      const state = yield select(restapiState);
      _tags = state.tags.bulk[0].data.records;
    }

    if (kbs.length) {
      yield all(kbs.map((kb, index) => put(actionTypes.bpsGet(kb, `KBS_${index}`))));
      yield all(kbs.map((kb, index) => take(`KBS_${index}`)));
      const state = yield select(restapiState);
      _kbs = kbs.map(kb => state.bestpractices.get[kb]);
    }

    if (sections.length) {
      yield all(sections.map((section, index) => put(actionTypes.sectionsGet(section, `SECTIONS_${index}`))));
      yield all(sections.map((section, index) => take(`SECTIONS_${index}`)));
      const state = yield select(restapiState);
      _sections = sections.map(sectionId => state.sections.get[sectionId].data);
    }

    const data = {
      template,
      tags: _tags,
      kbs: _kbs,
      sections: _sections
    };
    yield put(restapiData(data, URI, METHOD, action.data));
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, action.data));
    yield put(actionTypes.restapiError(error, URI, METHOD, action.data));
  }
}

export function* assessmentTemplateSaveWatcherSaga() {
  yield takeLatest([ASSESSMENT_TEMPLATE_POST], assessmentTemplateSaveEffectSage);
}

export function* assessmentTemplateGetWatcherSaga() {
  yield takeLatest([ASSESSMENT_TEMPLATE_GET], assessmentTemplateGetEffectSage);
}
