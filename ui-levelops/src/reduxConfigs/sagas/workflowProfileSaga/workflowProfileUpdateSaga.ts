import { issueContextTypes, severityTypes } from "bugsnag";
import { transformWorkflowProfileData } from "configurations/pages/lead-time-profiles/helpers/leadTime.helper";
import { handleError } from "helper/errorReporting.helper";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { workflowProfileActions } from "reduxConfigs/actions/actionTypes";
import {
  saveWorkflowProfileFailedAction,
  saveWorkflowProfileSuccessfulAction
} from "reduxConfigs/actions/restapi/workFlowNewAction";
import { IssueManagementWorkItemFieldListService, WorkflowProfileServices } from "services/restapi";
import { PROFILE_KEYS } from "./constants";
import { cloneDeep, get, set, uniq } from "lodash";
import { Integration } from "model/entities/Integration";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { getUpdatedFilters } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

function* workflowProfileUpdateSaga(action: any) {
  try {
    const workflowServices = new WorkflowProfileServices();
    const workFlowProfile = action.data;

    const integrationsIds = PROFILE_KEYS.reduce((acc: string[], next: string) => {
      const ids = get(workFlowProfile, [next, "integration_ids"], []).map((id: any) => id?.toString());
      if (ids?.length) {
        return [...acc, ...ids];
      }
      const integrationId = get(workFlowProfile, [next, "integration_id"]);
      return [...acc, integrationId?.toString()];
    }, []).filter(integrationData => integrationData !== undefined);

    const cachedIntegrations: Integration[] = yield select(cachedIntegrationsListSelector, {
      integration_ids: integrationsIds
    });
    let newWorkflowProfile = { ...workFlowProfile };
    const azureIntegrationIds = uniq(
      integrationsIds.filter(
        (id: string) => cachedIntegrations.find((cid: any) => cid.id === id)?.application === IntegrationTypes.AZURE
      )
    );
    let fieldListRecords: IntegrationTransformedCFTypes[] = [];
    if (azureIntegrationIds.length) {
      const issueManageFieldsService = new IssueManagementWorkItemFieldListService();
      const response: { data: { records: any[] } } = yield call(issueManageFieldsService.list, {
        filter: { integration_ids: azureIntegrationIds, transformedCustomFieldData: true }
      });
      fieldListRecords = get(response, ["data", "records"], []);
      newWorkflowProfile = Object.keys(workFlowProfile).reduce((acc: any, next: string) => {
        if (PROFILE_KEYS.includes(next)) {
          const integrationId = get(workFlowProfile, [next, "integration_id"]);
          const isAzureIntegration = azureIntegrationIds.includes(integrationId);
          if (!isAzureIntegration) {
            return { ...acc, [next]: workFlowProfile[next] };
          } else {
            const newProfile = cloneDeep(workFlowProfile[next]);
            if (next === "change_failure_rate") {
              const failedFilters = get(newProfile, ["filters", "failed_deployment", "filter"], {});
              const totalFilters = get(newProfile, ["filters", "total_deployment", "filter"], {});
              const filters = getUpdatedFilters(failedFilters, fieldListRecords);
              set(newProfile, ["filters", "failed_deployment", "filter"], filters);
              if (totalFilters.length) {
                const filters = getUpdatedFilters(totalFilters, fieldListRecords);
                set(newProfile, ["filters", "total_deployment", "filter"], filters);
              }
              return { ...acc, [next]: newProfile };
            } else {
              const filters = get(newProfile, ["filters", next, "filter"], {});
              const _filters = getUpdatedFilters(filters, fieldListRecords);
              set(newProfile, ["filters", next, "filter"], _filters);
              return { ...acc, [next]: newProfile };
            }
          }
        }
        return { ...acc, [next]: workFlowProfile[next] };
      }, {});
    }
    let finalUpdateJson = transformWorkflowProfileData(newWorkflowProfile);
    // @ts-ignore
    const response = yield call(workflowServices.update, action.id, finalUpdateJson);
    if (response.error) {
      yield put(saveWorkflowProfileFailedAction(response.error));
    } else {
      yield put(saveWorkflowProfileSuccessfulAction(response.data?.id));
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to update the profile.",
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.VELOCITY,
        data: { e, action }
      }
    });

    yield put(saveWorkflowProfileFailedAction(e));
  }
}

export function* workflowProfileUpdateWatcherSaga() {
  yield takeLatest(workflowProfileActions.WORKFLOW_PROFILE_UPDATE, workflowProfileUpdateSaga);
}
