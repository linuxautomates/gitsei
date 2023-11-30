import { get } from "lodash";
import { call, put, select, take, takeLatest } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import {
  trellisProfileRequireIntegrationAction,
  trellisProfilesListLoadFailedAction,
  trellisProfilesListLoadSuccessfullAction,
  trellisProfileListAlreadyPresentAction
} from "reduxConfigs/actions/restapi/trellisProfileActions";
import { integrationsList } from "reduxConfigs/actions/restapi/integrationActions";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import { _integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { TrellisProfileServices } from "services/restapi";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const ON_INTEGRATIONS_LOADED = "ON_INTEGRATIONS_LOADED";

function* trellisProfileListSaga(action: any) {
  try {
    const profilesState: TrellisProfilesListState = yield select(trellisProfileListSelector);
    const profiles = profilesState.data?.records;
    if (profiles?.length && !action.forceReload) {
      yield put(trellisProfileListAlreadyPresentAction());
      return;
    }
    // Get Integrations
    // @ts-ignore
    const integrationsListState = yield select(_integrationListSelector);
    let integrations = get(integrationsListState, ["0", "data", "records"], undefined);
    if (!integrations) {
      yield put(
        // @ts-ignore
        integrationsList({ filter: {} }, ON_INTEGRATIONS_LOADED, "0")
      );

      yield take(ON_INTEGRATIONS_LOADED);
      // @ts-ignore
      const integrationsListState = yield select(_integrationListSelector);
      integrations = get(integrationsListState, ["0", "data", "records"], undefined);
    }
    if (!integrations || integrations.length === 0) {
      yield put(trellisProfileRequireIntegrationAction());
      return;
    }

    const trellisServices = new TrellisProfileServices();

    // @ts-ignore
    const response = yield call(trellisServices.list, action.filters);
    if (response.error) {
      yield put(trellisProfilesListLoadFailedAction(response.error));
    }
    yield put(trellisProfilesListLoadSuccessfullAction(response.data));
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to load trellis profiles.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });

    yield put(trellisProfilesListLoadFailedAction(e));
  }
}

export function* trellisProfileListWatcherSaga() {
  yield takeLatest(trellisProfileActions.GET_TRELLIS_PROFILES_LIST, trellisProfileListSaga);
}
