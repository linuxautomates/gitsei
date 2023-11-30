import { issueContextTypes, severityTypes } from "bugsnag";
import { transformWorkflowProfileDataGet } from "configurations/pages/lead-time-profiles/helpers/leadTime.helper";
import { getAttributesForm } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { handleError } from "helper/errorReporting.helper";
import { get, uniq } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { workflowProfileActions } from "reduxConfigs/actions/actionTypes";
import {
  workflowProfileLoadFailedAction,
  workflowProfileLoadSuccessfulAction
} from "reduxConfigs/actions/restapi/workFlowNewAction";
import { _integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { IssueManagementWorkItemFieldListService, WorkflowProfileServices } from "services/restapi";
import { PROFILE_KEYS } from "./constants";
import { Integration } from "model/entities/Integration";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { cachedIntegrationEffectSaga } from "../integrations/cachedIntegrationSaga";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { IntegrationTypes } from "constants/IntegrationTypes";

function* workflowProfileDetailsSaga(action: any) {
  try {
    const workflowServices = new WorkflowProfileServices();

    // @ts-ignore
    const response = yield call(workflowServices.get, action.id);
    let newWorkFlowProfile = { ...response.data };
    const integrationsIds = PROFILE_KEYS.reduce((acc: string[], next: string) => {
      const ids = get(response.data, [next, "integration_ids"], []).map((id: any) => id?.toString());
      if (ids?.length) {
        return [...acc, ...ids];
      }
      const integrationId = get(response.data, [next, "integration_id"]);
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
    if (response.error) {
      yield put(workflowProfileLoadFailedAction(response.error));
    } else {
      let finalResponseData = transformWorkflowProfileDataGet(
        newWorkFlowProfile,
        action.basicStages,
        fieldListRecords,
        integrations
      );

      yield put(workflowProfileLoadSuccessfulAction(finalResponseData));
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to load the profile.",
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });

    yield put(workflowProfileLoadFailedAction(e));
  }
}

export function* workflowProfileDetailsWatcherSaga() {
  yield takeLatest(workflowProfileActions.WORKFLOW_PROFILE_READ, workflowProfileDetailsSaga);
}
