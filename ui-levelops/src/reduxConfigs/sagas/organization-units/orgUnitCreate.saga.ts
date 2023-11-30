import { notification } from "antd";
import {
  orgUnitJSONType,
  OrgUnitSectionPayloadType,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { ORGANIZATION_UNIT_SAVE } from "reduxConfigs/actions/actionTypes";
import { orgUnitCreateDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { transformDraftOrgUnitData } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import {
  OrganizationUnitDashboardAssociationService,
  OrganizationUnitService
} from "services/restapi/OrganizationUnit.services";
import { cloneDeep, forEach, get } from "lodash";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { IssueManagementWorkItemFieldListService } from "services/restapi";
import { IntegrationTypes } from "constants/IntegrationTypes";

function* createOrgUnitSaga(action: { type: string; id: string; messageFlag?: boolean }): any {
  const orgUnitService = new OrganizationUnitService();

  try {
    const orgUnitToCreate: orgUnitJSONType = yield select(orgUnitCreateDataSelect);

    const azureIntegrationIds: string[] = [];
    forEach(get(orgUnitToCreate, ["sections"], []), section => {
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

      forEach(get(orgUnitToCreate, ["sections"], []), section => {
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

      orgUnitToCreate["sections"] = newSections;
    }

    const response = yield call(orgUnitService.create as any, [transformDraftOrgUnitData(orgUnitToCreate)]);
    const success: string[] = get(response, ["data", "success"], []);

    if (success.length) {
      if (orgUnitToCreate?.dashboards?.length) {
        const orgUnitDashboardAssociationService = new OrganizationUnitDashboardAssociationService();
        let orgUnitState = yield call(orgUnitService.get, success[0], {});
        let orgUnitGot = get(orgUnitState, ["data"], undefined);
        const dashboards = (orgUnitToCreate?.dashboards ?? []).map((dash, idx) => ({
          ...dash,
          dashboard_order: idx + 1,
          ou_id: orgUnitGot?.ou_id ?? dash.ou_id
        }));
        yield call(orgUnitDashboardAssociationService.update, orgUnitGot?.ou_id, dashboards);
      }
      if (action?.messageFlag) {
        notification.success({ message: "Collection Created Successfully!" });
      }
      yield put(restapiData(response?.data ?? {}, "organization_unit_management", "create", action.id));
      yield put(restapiError(false, "organization_unit_management", "create", action.id));
      yield put(restapiLoading(false, "organization_unit_management", "create", action.id));
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to create collection",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* createOrgUnitSagaWatcher() {
  yield takeLatest(ORGANIZATION_UNIT_SAVE, createOrgUnitSaga);
}
