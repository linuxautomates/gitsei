import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { orgUnitJSONType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { handleError } from "helper/errorReporting.helper";
import { cloneDeep, get } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { ORGANIZATION_UNIT_CLONE } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import {
  OrganizationUnitDashboardAssociationService,
  OrganizationUnitService
} from "services/restapi/OrganizationUnit.services";
import { unsetKeysFromObject } from "utils/commonUtils";
import { restapiEffectSaga } from "../restapiSaga";

function* cloneOrgUnitSaga(action: { type: string; orgUnitToClone: orgUnitJSONType; id: string }): any {
  const orgUnitService = new OrganizationUnitService();

  try {
    notification.info({ message: "Cloning Collection..." });
    const orgUnitToClone = action.orgUnitToClone;
    if (!!orgUnitToClone) {
      let newOrgUnit = cloneDeep(orgUnitToClone);
      const nTags: any[] = (newOrgUnit as any)._tags || [];
      if (nTags.length) {
        newOrgUnit.tags = nTags.map(tag => tag?.key);
      }

      /** getting associated dashboards if any */
      yield call(restapiEffectSaga, {
        uri: "ous_dashboards",
        method: "list",
        data: { id: newOrgUnit?.ou_id, filter: { inherited: false }, page: 0, page_size: 1000 },
        id: newOrgUnit.id
      });

      let restState = yield select(restapiState);
      const associatedDashboards = get(
        restState,
        ["ous_dashboards", "list", newOrgUnit.id ?? "", "data", "records"],
        []
      );

      newOrgUnit = unsetKeysFromObject(["_tags", "id", "ou_id", "version", "path"], newOrgUnit);
      const response = yield call(orgUnitService.create as any, [newOrgUnit]);
      const newOrgUnitSuccessArray: string[] = get(response, ["data", "success"], []);

      if (newOrgUnitSuccessArray.length > 0) {
        if (associatedDashboards?.length) {
          const orgUnitDashboardAssociationService = new OrganizationUnitDashboardAssociationService();
          let orgUnitState = yield call(orgUnitService.get, newOrgUnitSuccessArray[0], {});
          let orgUnitGot = get(orgUnitState, ["data"], undefined);
          const newAssociatedDashboards: OUDashboardType[] = associatedDashboards.map((dashboard: OUDashboardType) => ({
            ...(dashboard ?? {}),
            ou_id: orgUnitGot?.ou_id
          }));
          yield call(orgUnitDashboardAssociationService.update, orgUnitGot?.ou_id, newAssociatedDashboards);
        }
        notification.success({ message: "Collection Cloned successfully" });
        yield put(restapiData(response?.data ?? {}, "organization_unit_management", "create", action.id));
        yield put(restapiError(false, "organization_unit_management", "create", action.id));
        yield put(restapiLoading(false, "organization_unit_management", "create", action.id));
      }
    } else {
      notification.error({ message: "Collection not found" });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to clone collection",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.FILES,
        data: { e, action }
      }
    });
  }
}

export function* cloneOrgUnitSagaWatcher() {
  yield takeEvery(ORGANIZATION_UNIT_CLONE, cloneOrgUnitSaga);
}
