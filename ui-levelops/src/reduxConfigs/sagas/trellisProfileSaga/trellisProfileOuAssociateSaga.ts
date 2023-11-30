import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { handleError } from "helper/errorReporting.helper";
import { call, select, takeLatest } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import { TrellisProfileServices } from "services/restapi";

const uri: string = "dev_productivity_profile";

function* trellisProfileOuAssociateEffectSaga(action: any) {
  const { profileId, orgId, orgName } = action;

  try {
    // @ts-ignore
    const profilesState: TrellisProfilesListState = yield select(trellisProfileListSelector);
    const profiles = profilesState.data?.records;
    const trellisServices = new TrellisProfileServices();

    const profileToUpdate: any = profiles?.find((profile: RestTrellisScoreProfile) => profile.id === profileId);
    if (profileToUpdate) {
      profileToUpdate.associated_ou_ref_ids = [...(profileToUpdate.associated_ou_ref_ids || []), orgId];
      // @ts-ignore
      const response = yield call(trellisServices.update, action.profileId, profileToUpdate);
      if (!response.error) {
        notification.success({ message: `Profile association for ${orgName} has been done successfully.` });
      }
    } else {
      notification.error({ message: `The profile not found` });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to associate the profile to OU.",
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* trellisProfileOuAssociateSaga() {
  yield takeLatest(trellisProfileActions.ASSOCIATE_OU_TO_PROFILE, trellisProfileOuAssociateEffectSaga);
}
