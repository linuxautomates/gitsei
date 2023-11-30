import { issueContextTypes, severityTypes } from "bugsnag";
import { OUDashboardType } from "configurations/configuration-types/OUTypes";
import { ORG_UNIT_DASHBOARDS_ASSOCIATION_ID } from "configurations/pages/Organization/Constants";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, takeEvery } from "redux-saga/effects";
import { ORGANZATION_UNIT_DASHBOARDS } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import {
  OrganizationUnitDashboardAssociationService,
  OrganizationUnitService
} from "services/restapi/OrganizationUnit.services";

export function* orgUnitDashboardsSaga(action: any): any {
  const { parent_ou_id } = action;
  const uri = "ous_dashboards";
  let inheritedDashboards: OUDashboardType[] = [];
  const orgUnitService = new OrganizationUnitService();
  const orgUnitDashboardAssociationService = new OrganizationUnitDashboardAssociationService();

  try {
    /** getting inherited dashboards from parent OU */

    if (parent_ou_id) {
      let orgUnitState = yield call(orgUnitService.get, parent_ou_id, {});
      let orgUnitGot = get(orgUnitState, ["data"], undefined);
      const inheritedDashboardsState: { data: { records: Array<OUDashboardType> } } = yield call(
        orgUnitDashboardAssociationService.list,
        {
          id: orgUnitGot?.ou_id,
          filter: { inherited: true },
          page: 0,
          page_size: 1000
        }
      );

      inheritedDashboards = get(inheritedDashboardsState, ["data", "records"], []);
      inheritedDashboards = inheritedDashboards.reduce((acc: OUDashboardType[], dash: OUDashboardType) => {
        const nDash: OUDashboardType = {
          ...(dash ?? {}),
          dashboard_id: dash?.dashboard_id?.toString() // typecasting dashboard_id to string
        };
        acc.push(nDash);
        return acc;
      }, []);
    }

    yield put(
      genericRestAPISet({ inherited_dashboards: inheritedDashboards }, uri, "list", ORG_UNIT_DASHBOARDS_ASSOCIATION_ID)
    );
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* OrgUnitDashboardsSagaWatcher() {
  yield takeEvery(ORGANZATION_UNIT_DASHBOARDS, orgUnitDashboardsSaga);
}
