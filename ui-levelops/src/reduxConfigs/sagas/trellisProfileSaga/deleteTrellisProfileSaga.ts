import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest, select } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import { trellisProfilesListLoadSuccessfullAction } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import { TrellisProfileServices } from "services/restapi";

function* deleteTrellisProfileEffectSaga(action: any): any {
  const { id } = action;
  try {
    // @ts-ignore
    const profilesState: TrellisProfilesListState = yield select(trellisProfileListSelector);
    const profiles = profilesState.data?.records;
    const trellisServices = new TrellisProfileServices();
    yield call(trellisServices.delete, id);

    const _profiles = profiles?.filter((profile: { id: any }) => profile.id !== id);
    yield put(
      trellisProfilesListLoadSuccessfullAction({
        ...profilesState.data,
        records: _profiles,
        count: (profilesState.data?.count || 0) - 1
      })
    );

    notification.success({ message: "Profile Deleted successfully" });
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to delete profile",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* deleteTrellisProfileSagaWatcherSaga() {
  yield takeLatest(trellisProfileActions.TRELLIS_PROFILE_DELETE, deleteTrellisProfileEffectSaga);
}
