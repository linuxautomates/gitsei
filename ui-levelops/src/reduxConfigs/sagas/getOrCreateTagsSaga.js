import { all, call, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { TAGS_GET_OR_CREATE } from "reduxConfigs/actions/actionTypes";
import { RestTags } from "classes/RestTags";

const restapiState = state => state.restapiReducer;

export function* getOrCreateTagsEffectSage(action) {
  const URI = "tags";
  const METHOD = "getOrCreate";
  const ID = 0;
  let error = false;

  try {
    const { tag_names } = action;
    if (!tag_names || tag_names.length === 0) {
      return;
    }

    const toCreateTagNames = tag_names.filter(tag => tag.includes("create:"));
    const remainingTagNames = tag_names.filter(tag => !tag.includes("create:"));

    if (toCreateTagNames.length === tag_names.length) {
      yield call(createTagsByNames, toCreateTagNames);
      const state = yield select(restapiState);
      const newlyCreatedTags = toCreateTagNames.map(tag => ({
        id: state.tags.create[tag].data.id,
        name: tag
      }));
      yield put(actionTypes.restapiData(newlyCreatedTags, URI, METHOD, ID));
      if (action.hasOwnProperty("complete") && action.complete !== null) {
        yield put({ type: action.complete });
      }
      return;
    }
    const TAG_CREATED_ACTION = "TAG_CREATED_ACTION";
    yield all(
      remainingTagNames.map(tag => {
        return put(
          actionTypes.tagsList(
            {
              filter: {
                partial: {
                  name: tag
                }
              }
            },
            tag,
            `${TAG_CREATED_ACTION}_${tag}`
          )
        );
      })
    );
    yield all(
      remainingTagNames.map(tag => {
        return take(`${TAG_CREATED_ACTION}_${tag}`);
      })
    );

    const apiState = yield select(restapiState);
    const { newTagNames, alreadyExistingTags } = remainingTagNames.reduce(
      (prevValue, tag) => {
        const existingTag = apiState.tags.list[tag].data.records[0];
        if (existingTag && tag === existingTag.name) {
          prevValue.alreadyExistingTags.push(existingTag);
        } else {
          prevValue.newTagNames.push(tag);
        }
        return prevValue;
      },
      {
        newTagNames: [],
        alreadyExistingTags: []
      }
    );

    yield call(createTagsByNames, [...toCreateTagNames, ...newTagNames]);
    const newState = yield select(restapiState);
    const newlyCreatedTags = [...toCreateTagNames, ...newTagNames].map(tag => ({
      id: newState.tags.create[tag].data.id,
      name: tag
    }));
    yield put(actionTypes.restapiData([...newlyCreatedTags, ...alreadyExistingTags], URI, METHOD, ID));
    if (action.hasOwnProperty("complete") && action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, ID));
    yield put(actionTypes.restapiError(error, URI, METHOD, ID));
  }
}

export function* createTagsByNames(names) {
  const TAG_CREATED_ACTION = "TAG_CREATED_ACTION";
  yield all(
    names.map(tag => {
      const newTag = new RestTags();
      newTag.name = tag.includes("create:") ? tag.replace("create:", "") : tag;
      return put(actionTypes.tagsCreate(newTag, tag, `${TAG_CREATED_ACTION}_${tag}`));
    })
  );

  yield all(names.map(tag => take(`${TAG_CREATED_ACTION}_${tag}`)));
}

export function* getOrCreateTagsWatcherSaga() {
  yield takeLatest([TAGS_GET_OR_CREATE], getOrCreateTagsEffectSage);
}
