import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { cloneDeep, get, uniq } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { GENERIC_STORED_IDS_MAP } from "reduxConfigs/actions/actionTypes";
import { formUpdateObj } from "reduxConfigs/actions/formActions";
import { sanitizeObject } from "../../utils/commonUtils";
import { genericIdsMapSaga, idsMapFormKeys, IdsMapFormType, LabelIdMap } from "./genericIdsMap.saga";

const formState = (state: any) => state.formReducer;

export function* genericStoredIdsMapSaga(action: any): any {
  try {
    const { filters, formName } = action;
    const formRestState = yield select(formState);
    const previousForm: IdsMapFormType = get(formRestState, formName, undefined);
    if (!previousForm) {
      yield call(genericIdsMapSaga, action);
      return;
    }

    let remainingKeys = {};

    idsMapFormKeys.forEach((key: string) => {
      const formDataKeys: string[] = uniq(get(filters, key, []));
      if (formDataKeys.length) {
        const remaining_ids: string[] = [];
        formDataKeys.forEach((id: string) => {
          const hasData = get(previousForm, key, [])
            .map((data: LabelIdMap) => data.id)
            .includes(id);
          if (!hasData) remaining_ids.push(id);
        });
        remainingKeys = { ...remainingKeys, [key]: remaining_ids };
      }
    });

    remainingKeys = sanitizeObject(remainingKeys);

    if (Object.keys(remainingKeys).length) {
      yield call(genericIdsMapSaga, { ...action, filters: sanitizeObject(remainingKeys) });

      const currentFormRestState = yield select(formState);
      const currentFormState: IdsMapFormType = cloneDeep(get(currentFormRestState, formName, undefined));

      let mergedData: IdsMapFormType = {};

      idsMapFormKeys.forEach((key: string) => {
        const previousData = get(previousForm, key, []);
        const currentData = get(currentFormState, key, []);
        mergedData = { ...mergedData, [key]: [...previousData, ...currentData] };
      });
      yield put(formUpdateObj(formName, sanitizeObject(mergedData)));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.APIS,
        data: { e, action }
      }
    });
  }
}

export function* genericStoredIdsMapWatcherSaga() {
  yield takeEvery([GENERIC_STORED_IDS_MAP], genericStoredIdsMapSaga);
}
