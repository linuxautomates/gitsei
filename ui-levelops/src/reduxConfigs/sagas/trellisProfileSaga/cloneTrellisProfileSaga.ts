import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { handleError } from "helper/errorReporting.helper";
import { cloneDeep, get } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import { trellisProfilesListLoadSuccessfullAction } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import { TrellisProfileServices } from "services/restapi";

const uri: string = "dev_productivity_profile";

function* trellisProfileCloneEffectSaga(action: any): any {
  const { id } = action;

  try {
    // @ts-ignore
    const profilesState: TrellisProfilesListState = yield select(trellisProfileListSelector);
    const profiles = profilesState.data?.records;
    const trellisServices = new TrellisProfileServices();

    const profileToClone: RestTrellisScoreProfile | undefined = profiles?.find(
      (profile: RestTrellisScoreProfile) => profile.id === id
    );
    if (profileToClone) {
      notification.info({ message: `Cloning ${profileToClone.name}...` });
      const newProfile = cloneDeep(profileToClone);
      newProfile.name = `Copy of ${newProfile.name}`;
      // @ts-ignore
      const createResponse = yield call(trellisServices.create, newProfile);

      const newProfileId = get(createResponse, ["data", "id"], undefined);

      if (newProfileId) {
        // @ts-ignore
        const getNewProfileResponse = yield call(trellisServices.get, newProfileId);
        const clonedProfile = getNewProfileResponse.data;
        const _profiles = [...(profiles ?? []), clonedProfile];

        yield put(
          trellisProfilesListLoadSuccessfullAction({
            ...profilesState.data,
            records: _profiles,
            count: (profilesState.data?.count || 0) + 1
          })
        );
        notification.success({ message: `${profileToClone.name} Cloned successfully.` });
      }
    } else {
      notification.error({ message: `The profile not found` });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to clone the profile.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* trellisProfileCloneWatcherSaga() {
  yield takeLatest(trellisProfileActions.TRELLIS_PROFILE_CLONE, trellisProfileCloneEffectSaga);
}
