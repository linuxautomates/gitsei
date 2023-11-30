import { put, select, takeLatest, all, take } from "redux-saga/effects";
import { KBS_GET_OR_CREATE } from "reduxConfigs/actions/actionTypes";
import { KB_CREATE_ACTION } from "./importAssessmentSaga";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { RestKB } from "classes/RestKB";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

/*
TODO : once lev-1721 is complete,we need to make globally unique KBs only, for now they have
 to be csv level unique
*/
export function* getOrCreateKBEffectSage(action) {
  const kbs = action.data;
  const createdState = yield select(state => state.restapiReducer?.bestpractices?.create);
  const filteredLinks = [];
  yield all(
    kbs.map(kb => {
      const { value, name } = kb;
      if (!createdState?.[`${KB_CREATE_ACTION}_${value}`]?.data?.id) {
        try {
          filteredLinks.push(`${KB_CREATE_ACTION}_${value}`);
          return put(
            actionTypes.bpsCreate(
              constructKB(name, value),
              `${KB_CREATE_ACTION}_${value}`,
              `${KB_CREATE_ACTION}_${value}`
            )
          );
        } catch (e) {
          handleError({
            bugsnag: {
              message: e?.message,
              severity: severityTypes.ERROR,
              context: issueContextTypes.KNOWLEDGEBASE,
              data: { e, action }
            }
          });
          return;
        }
      }
    })
  );
  yield all(
    filteredLinks.map(kb => {
      return take(kb);
    })
  );
  yield put({ type: action.complete });
}

function constructKB(name, link) {
  return new RestKB({ metadata: "", name, tags: [], type: "LINK", value: link });
}

export function* getOrCreateKBWatcherSaga() {
  yield takeLatest([KBS_GET_OR_CREATE], getOrCreateKBEffectSage);
}
