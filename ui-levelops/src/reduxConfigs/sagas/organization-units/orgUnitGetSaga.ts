import { v1 as uuid } from "uuid";
import { forEach, get, unset } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { ORGANIZATION_UNIT_GET, ORGANIZATION_UNIT_UTILITY_SAGA } from "reduxConfigs/actions/actionTypes";
import { getAttributesForm, transformUsersSelection } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { OrgUnitSectionPayloadType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { orgUnitUtilityEffectSaga } from "./orgUnitUtilitySaga";
import { ORG_UNIT_UTILITIES } from "configurations/pages/Organization/Constants";
import { orgUnitUtilityState } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import {
  IssueManagementWorkItemFieldListService,
  OrganizationUnitDashboardAssociationService,
  OrganizationUnitService
} from "services/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { genericList } from "reduxConfigs/actions/restapi";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { IntegrationTypes } from "constants/IntegrationTypes";

function* orgUnitGetSaga(action: { type: string; uri: string; id: string; queryparams: { [x: string]: any } }): any {
  const { id, uri, queryparams } = action;
  queryparams.isAllOrgUsersRequired =
    queryparams?.isAllOrgUsersRequired !== undefined ? queryparams?.isAllOrgUsersRequired : true;
  const orgUnitService = new OrganizationUnitService();
  try {
    let utilities = yield select(orgUnitUtilityState);
    let users = utilities?.users || [];
    // Call org/users/list only when it's required. Flag isAllOrgUsersRequired value false added in Dev productivity page where we don't need below call currently
    if (!Object.keys(utilities || {}).length && queryparams?.isAllOrgUsersRequired) {
      yield call(orgUnitUtilityEffectSaga, { type: ORGANIZATION_UNIT_UTILITY_SAGA, id: ORG_UNIT_UTILITIES });
      let { users: curActiveUsers } = yield select(orgUnitUtilityState);
      users = curActiveUsers || [];
    }

    let orgUnitState = yield call(orgUnitService.get, id, queryparams);
    let orgUnitGot = get(orgUnitState, ["data"], undefined);
    if (orgUnitGot) {
      /** getting already associated dashboards */
      const orgUnitDashboardAssociationService = new OrganizationUnitDashboardAssociationService();
      const specificDashboardsState: { data: { records: Array<OUDashboardType> } } = yield call(
        orgUnitDashboardAssociationService.list,
        {
          id: orgUnitGot?.ou_id,
          filter: { inherited: false },
          page: 0,
          page_size: 1000
        }
      );
      const specificDashboards: OUDashboardType[] = get(specificDashboardsState, ["data", "records"], []);
      orgUnitGot["dashboards"] = specificDashboards
        .reduce((acc: OUDashboardType[], dash: OUDashboardType) => {
          const nDash: OUDashboardType = {
            ...(dash ?? {}),
            dashboard_id: dash?.dashboard_id?.toString() // typecasting dashboard_id to string
          };
          acc.push(nDash);
          return acc;
        }, [])
        .sort((d1, d2) => {
          if (d1.dashboard_order && d2.dashboard_order) {
            return d1.dashboard_order - d2.dashboard_order;
          }
          return 0;
        });

      /** checking if children exits */
      const childsState = yield call(
        orgUnitService.list,
        {
          page: 0,
          page_size: 20,
          filter: { parent_ref_id: orgUnitGot?.id }
        },
        {}
      );

      const children = get(childsState, ["data", "records"], []);
      orgUnitGot["has_childs"] = !!children.length;

      let curActiveUserIds: string[] = users.map((user: { id: string }) => user.id);
      // showing users only from current active version
      orgUnitGot["managers"] = get(orgUnitGot, ["managers"], []).filter((manager: { id: string }) =>
        curActiveUserIds.includes(manager?.id)
      );

      const tagIds = get(orgUnitGot, ["tags"], []);
      if (tagIds.length) {
        yield put(genericList("tags", "list", { filter: { tag_ids: tagIds } }, null, id, true));
      }

      orgUnitGot["default_section"] = transformUsersSelection(
        get(orgUnitGot, ["default_section"], {}),
        curActiveUserIds
      );
      let newSections: OrgUnitSectionPayloadType[] = [];

      const azureIntegrationIds: string[] = [];
      forEach(get(orgUnitGot, ["sections"], []), section => {
        const integrations = get(section, ["integrations"], {});
        const integrationIds = Object.keys(integrations) || [];

        if (integrationIds.length) {
          const typeId = integrationIds[0];
          const value = integrations[typeId];
          if (value?.type === IntegrationTypes.AZURE) {
            azureIntegrationIds.push(typeId);
          }
        }
      });

      let fieldListRecords: IntegrationTransformedCFTypes[] = [];
      if (azureIntegrationIds.length) {
        const issueManageFieldsService = new IssueManagementWorkItemFieldListService();
        const response: { data: { records: any[] } } = yield call(issueManageFieldsService.list, {
          filter: { integration_ids: azureIntegrationIds, transformedCustomFieldData: true }
        });
        fieldListRecords = get(response, ["data", "records"], []);
      }

      forEach(get(orgUnitGot, ["sections"], []), section => {
        let newSection = transformUsersSelection(section, curActiveUserIds);
        let usersData: any = {};
        const userKeys = ["users", "dynamic_user_definition"];
        let keysToUnset = [...userKeys, "integrations"];
        forEach(Object.keys(newSection), key => {
          if (userKeys.includes(key)) {
            usersData[key] = newSection[key];
          }
        });

        usersData = sanitizeObjectCompletely(usersData);

        if (Object.keys(usersData).length) {
          newSection["user_groups"] = [{ id: uuid(), ...usersData }];
        }
        const integrations = get(newSection, ["integrations"], {});
        const integrationIds = Object.keys(integrations) || [];

        if (integrationIds.length) {
          const typeId = integrationIds[0];
          const value = integrations[typeId];
          newSection["type"] = `${get(value, ["type"], "")}@${typeId}`;
          newSection["integration"] = {
            id: uuid(),
            filters: getAttributesForm(get(value, ["filters"], {}), fieldListRecords ?? [])
          };
        }

        forEach(keysToUnset, key => unset(newSection, key));
        newSections.push(newSection as OrgUnitSectionPayloadType);
      });

      orgUnitGot["sections"] = newSections;

      if (!orgUnitGot.parent_ref_id) {
        orgUnitGot.is_parent = true;
      }

      yield put(genericRestAPISet(orgUnitGot, uri, "get", id));
    }
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

export function* orgUnitGetSagaWatcher() {
  yield takeLatest(ORGANIZATION_UNIT_GET, orgUnitGetSaga);
}
