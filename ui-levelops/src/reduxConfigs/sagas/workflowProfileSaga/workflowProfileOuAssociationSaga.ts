import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { select, takeLatest, call } from "redux-saga/effects";
import { workflowProfileActions } from "reduxConfigs/actions/actionTypes";
import { getVeloCityConfigById } from "reduxConfigs/selectors/velocityConfigs.selector";
import { WorkflowProfileServices } from "services/restapi";

const uri: string = "velocity_configs";

function* workflowProfileOuAssociationSaga(action: any) {
  const { profileId, orgId, orgName } = action;
  const workflowServices = new WorkflowProfileServices();
  try {
    // @ts-ignore
    const configToUpdate = yield select(getVeloCityConfigById, {
      id: profileId
    });

    if (!configToUpdate.is_new) {
      notification.info({ message: "Can not associate OU to old profile." });
      return;
    }
    const updatedConfig = {
      ...configToUpdate,
      associated_ou_ref_ids: [...(configToUpdate.associated_ou_ref_ids || []), orgId]
    };
    // @ts-ignore
    const response = yield call(workflowServices.update, profileId, updatedConfig);

    if (!response.error) {
      notification.success({ message: `Profile association for ${orgName} has been done successfully.` });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to associate the profile to OU.",
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.VELOCITY,
        data: { e, action }
      }
    });
  }
}

export function* workflowProfileOuAssociationSagaWatcher() {
  yield takeLatest(workflowProfileActions.ASSOCIATE_OU_TO_PROFILE, workflowProfileOuAssociationSaga);
}
