import { notification } from "antd";
import { cloneDeep, forEach, get, unset } from "lodash";
import {
  orgUnitJSONType,
  OrgUnitSectionPayloadType,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { ORGANIZATION_UNIT_UPDATE } from "reduxConfigs/actions/actionTypes";
import { getSelectedOU, orgUnitGetDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { transformDraftOrgUnitData } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { restapiData, restapiError, restapiLoading, setSelectedChildId } from "reduxConfigs/actions/restapi";
import {
  OrganizationUnitDashboardAssociationService,
  OrganizationUnitService
} from "services/restapi/OrganizationUnit.services";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { IssueManagementWorkItemFieldListService } from "services/restapi";
import { IntegrationTypes } from "constants/IntegrationTypes";

function* updateOrgUnitSaga(action: { type: string; id: string; messageFlag?: boolean }): any {
  const orgUnitService = new OrganizationUnitService();
  try {
    const selectedOU = yield select(getSelectedOU);
    const orgUnitToUpdateState: { data: orgUnitJSONType } = yield select(orgUnitGetDataSelect, { id: action.id });
    const orgUnitToUpdate: orgUnitJSONType = get(orgUnitToUpdateState, ["data"], {});
    if (orgUnitToUpdate) {
      let nOrgUnitToUpdate = cloneDeep(orgUnitToUpdate);
      unset(nOrgUnitToUpdate, "version");

      const azureIntegrationIds: string[] = [];
      forEach(get(nOrgUnitToUpdate, ["sections"], []), section => {
        const integrationData = section?.type?.split("@");
        if (integrationData.length > 1 && integrationData[0] === IntegrationTypes.AZURE) {
          azureIntegrationIds.push(integrationData[1]);
        }
      });

      let fieldListRecords: IntegrationTransformedCFTypes[] = [];
      if (azureIntegrationIds.length) {
        const issueManageFieldsService = new IssueManagementWorkItemFieldListService();
        const response: { data: { records: any[] } } = yield call(issueManageFieldsService.list, {
          filter: { integration_ids: azureIntegrationIds, transformedCustomFieldData: true }
        });
        fieldListRecords = get(response, ["data", "records"], []);

        let newSections: OrgUnitSectionPayloadType[] = [];

        forEach(get(nOrgUnitToUpdate, ["sections"], []), section => {
          const newSection = cloneDeep(section);
          const nfilters = cloneDeep(newSection?.integration?.filters || []);
          if (nfilters.length) {
            forEach(nfilters, (nfield: sectionSelectedFilterType) => {
              if (!nfield?.metadata) {
                const fieldListCustom: IntegrationTransformedCFTypes | undefined = (fieldListRecords ?? []).find(
                  (field: IntegrationTransformedCFTypes) => field?.field_key === nfield.key
                );
                if (fieldListCustom) nfield.metadata = fieldListCustom?.metadata;
              }
            });
            newSection.integration.filters = nfilters;
          }
          newSections.push(newSection);
        });

        nOrgUnitToUpdate["sections"] = newSections;
      }

      /** updating collection */

      const response = yield call(orgUnitService.update, action.id, [transformDraftOrgUnitData(nOrgUnitToUpdate)]);
      const status: string = get(response, ["data"], "false");
      if (status === "ok") {
        /** updating collection with new dashboard mapping */
        let orgUnitState = yield call(orgUnitService.get, orgUnitToUpdate?.id ?? "", {});
        let orgUnitGot = get(orgUnitState, ["data"], undefined);
        if (selectedOU?.id === orgUnitToUpdate?.id) {
          yield put(setSelectedChildId(orgUnitGot, "selected-OU"));
        }
        const orgUnitDashboardAssociationService = new OrganizationUnitDashboardAssociationService();
        const dashboards = (orgUnitToUpdate?.dashboards ?? []).map((dash, idx) => ({
          ...dash,
          dashboard_order: idx + 1,
          ou_id: orgUnitGot?.ou_id ?? dash.ou_id
        }));
        yield call(orgUnitDashboardAssociationService.update, orgUnitGot?.ou_id ?? "", dashboards);
        if (action?.messageFlag) {
          notification.success({ message: "Collection Updated Successfully" });
        }
        yield put(restapiData("ok", "organization_unit_management", "update", action.id));
        yield put(restapiError(false, "organization_unit_management", "update", action.id));
        yield put(restapiLoading(false, "organization_unit_management", "update", action.id));
      }
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to update collection",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* updateOrgUnitSagaWatcher() {
  yield takeLatest(ORGANIZATION_UNIT_UPDATE, updateOrgUnitSaga);
}
