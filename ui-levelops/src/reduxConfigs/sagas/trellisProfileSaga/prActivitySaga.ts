import { issueContextTypes, severityTypes } from "bugsnag";
import { DEV_PRODUCTIVITY_USER_PR_ACTIVITY } from "dashboard/pages/scorecard/components/PRActivity/helpers";
import { handleError } from "helper/errorReporting.helper";
import { call, takeLatest } from "redux-saga/effects";
import { DEV_PRODUCTIVITY_PR_ACTIVITY } from "reduxConfigs/actions/actionTypes";
import { restapiEffectSaga } from "../restapiSaga";

function* devProductivityPRActivityEffectSaga(action: any) {
  try {
    const ouProfile = action?.profileKey?.ou_ref_ids
      ? { ...action?.profileKey }
      : { dev_productivity_profile_id: action.trellis_profile_id };
    yield call(restapiEffectSaga, {
      uri: DEV_PRODUCTIVITY_USER_PR_ACTIVITY,
      method: "list",
      id: action.id,
      data: {
        filter: {
          user_id_type: action.user_id_type,
          user_id: action.user_id,
          time_range: action.time_range,
          ...ouProfile
        },
        page: 0,
        page_size: 100
      }
    });
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* devProductivityPRActivityWatcherSaga() {
  yield takeLatest(DEV_PRODUCTIVITY_PR_ACTIVITY, devProductivityPRActivityEffectSaga);
}
