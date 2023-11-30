import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { handleError } from "helper/errorReporting.helper";
import { get, map, uniq } from "lodash";
import { put, select, take, takeLatest, call } from "redux-saga/effects";
import { workflowProfileActions, WORKFLOW_PROFILE_CLONE } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { createWorkflowProfileAction } from "reduxConfigs/actions/restapi/workFlowNewAction";
import { WorkflowSavingState } from "reduxConfigs/reducers/workflowProfileReducer";
import {
  VELOCITY_CONFIG_LIST_ID,
  velocityConfigsRestListSelector
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { workflowProfileSavingStatusSelector } from "reduxConfigs/selectors/workflowProfileSelectors";
import { IssueManagementWorkItemFieldListService, WorkflowProfileServices } from "services/restapi";
import { PROFILE_KEYS } from "./constants";
import { cachedIntegrationEffectSaga } from "../integrations/cachedIntegrationSaga";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { Integration } from "model/entities/Integration";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { transformWorkflowProfileDataGet } from "configurations/pages/lead-time-profiles/helpers/leadTime.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const uri: string = "velocity_configs";

function* cloneWorkflowProfileSaga(action: any) {
  const { id } = action;
  const workflowServices = new WorkflowProfileServices();
  try {
    notification.info({ message: "Cloning Profile..." });
    // @ts-ignore
    const configs = yield select(velocityConfigsRestListSelector, {
      id: VELOCITY_CONFIG_LIST_ID
    });

    const configToClone = configs.find((config: RestVelocityConfigs | RestWorkflowProfile) => config.id === id);
    if (!configToClone.is_new) {
      notification.info({ message: "Can not clone old profile." });
      return;
    }
    const clonedConfig = {
      ...configToClone.postData,
      name: `Copy of ${configToClone.name}`,
      associated_ou_ref_ids: []
    };
    let newWorkFlowProfile = { ...clonedConfig };
    const integrationsIds = PROFILE_KEYS.reduce((acc: string[], next: string) => {
      const ids = get(clonedConfig, [next, "integration_ids"], []).map((id: any) => id?.toString());
      if (ids?.length) {
        return [...acc, ...ids];
      }
      const integrationId = get(clonedConfig, [next, "integration_id"]);
      return [...acc, integrationId?.toString()];
    }, []).filter(integrationData => integrationData !== undefined);
    yield call(cachedIntegrationEffectSaga as any, { payload: { method: "list", integrationIds: integrationsIds } });
    const integrations: Array<Integration> = yield select(cachedIntegrationsListSelector, {
      integration_ids: integrationsIds
    });
    const azureIntegrationIds = uniq(
      integrationsIds.filter(
        (id: string) => integrations.find((cid: any) => cid.id === id)?.application === IntegrationTypes.AZURE
      )
    );
    let fieldListRecords: IntegrationTransformedCFTypes[] = [];
    if (azureIntegrationIds.length) {
      const issueManageFieldsService = new IssueManagementWorkItemFieldListService();
      const response: { data: { records: any[] } } = yield call(issueManageFieldsService.list, {
        filter: { integration_ids: azureIntegrationIds, transformedCustomFieldData: true }
      });
      fieldListRecords = get(response, ["data", "records"], []);
    }
    let finalResponseData = transformWorkflowProfileDataGet(
      newWorkFlowProfile,
      action?.basicStages,
      fieldListRecords,
      integrations
    );
    yield put(createWorkflowProfileAction(finalResponseData));
    yield take([
      workflowProfileActions.WORKFLOW_PROFILE_SAVE_SUCCESSFUL,
      workflowProfileActions.WORKFLOW_PROFILE_SAVE_FAILED
    ]);
    const savedProfileState: WorkflowSavingState = yield select(workflowProfileSavingStatusSelector);

    if (savedProfileState.error) {
      notification.error({ message: "Failed to clone Profile" });
    }
    const newConfigId = savedProfileState.newId;
    if (newConfigId) {
      // @ts-ignore
      const response = yield call(workflowServices.get, newConfigId);

      const clonedConfig = get(response, ["data"], {});
      const _configs = [clonedConfig, ...map(configs, (config: any) => config.postData)];

      yield put(genericRestAPISet(_configs, uri, "list", VELOCITY_CONFIG_LIST_ID));
      notification.success({ message: "Profile Cloned successfully" });
    } else {
      notification.error({ message: "Profile not found" });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to clone profile",
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

export function* cloneWorkflowProfileSagaWatcher() {
  yield takeLatest(WORKFLOW_PROFILE_CLONE, cloneWorkflowProfileSaga);
}
