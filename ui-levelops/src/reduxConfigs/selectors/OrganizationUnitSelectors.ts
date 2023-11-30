import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import {
  NEW_ORG_UNIT_ID,
  ORG_UNIT_FILTER_VALUES,
  ORG_UNIT_UTILITIES
} from "configurations/pages/Organization/Constants";
import { DASHBOARD_OU_CHECK_LIST_ID } from "dashboard/pages/dashboard-view/constant";
import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getId = createParameterSelector((params: any) => params.id);
const getUtility = createParameterSelector((params: any) => params.utility);

const _orgUnitSelect = createSelector(restapiState, (data: any) => {
  return get(data, ["organization_unit_management"], { loading: true, error: false });
});

const _orgUnitCreateMethodSelect = createSelector(_orgUnitSelect, (data: any) => get(data, ["create"], {}));

const _orgUnitGetMethodSelect = createSelector(_orgUnitSelect, (data: any) => {
  return get(data, ["get"], {});
});

const _orgUnitListSelect = createSelector(_orgUnitSelect, (data: any) =>
  get(data, ["list"], { loading: true, error: false })
);

const _orgUnitBulkDeleteSelect = createSelector(_orgUnitSelect, (data: any) =>
  get(data, ["bulkDelete"], { loading: true, error: false })
);

export const orgUnitFiltersMapping = createSelector(_orgUnitSelect, (data: any) =>
  get(data, [ORG_UNIT_FILTER_VALUES], {})
);

export const orgUnitUtilityState = createSelector(_orgUnitSelect, (data: any) => {
  return get(data, [ORG_UNIT_UTILITIES], {});
});

export const orgUnitUpdateSelect = createSelector(_orgUnitSelect, getId, (data: any, id: string) =>
  get(data, ["update", id, "data"], "false")
);
export const orgUnitUpdateState = createSelector(_orgUnitSelect, getId, (data: any, id: string) =>
  get(data, ["update", id], { loading: true, error: false })
);

export const orgUnitCreateDataSelect = createSelector(_orgUnitCreateMethodSelect, (data: any) =>
  get(data, [NEW_ORG_UNIT_ID, "data"], {})
);

export const orgUnitGetDataSelect = createSelector(_orgUnitGetMethodSelect, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});

export const orgUnitCreateRestDataSelect = createSelector(orgUnitCreateDataSelect, (data: any) => {
  return new RestOrganizationUnit(data);
});

export const orgUnitGetRestDataSelect = createSelector(orgUnitGetDataSelect, (data: any) => {
  const ndata = get(data, ["data"], {});
  return new RestOrganizationUnit(ndata);
});

export const orgUnitCloneDataState = createSelector(_orgUnitCreateMethodSelect, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});

export const orgUnitCreateSuccessState = createSelector(_orgUnitCreateMethodSelect, getId, (data: any, id: string) =>
  get(data, [id, "data", "success"], [])
);

const _orgUnitVersionState = createSelector(restapiState, (data: any) =>
  get(data, ["organization_unit_version_control"], { loading: true, error: false })
);

const _orgUnitVersionGetState = createSelector(_orgUnitVersionState, (data: any) =>
  get(data, ["get"], { loading: true, error: false })
);

const _orgUnitVersionPostState = createSelector(_orgUnitVersionState, (data: any) =>
  get(data, ["list"], { loading: true, error: false })
);

export const orgUnitVersionPostIdState = createSelector(_orgUnitVersionPostState, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});

export const orgUnitVersionPostDataState = createSelector(orgUnitVersionPostIdState, (data: any) => {
  const activeVersion = get(data, ["data"], undefined);
  if (activeVersion === "") {
    return "ok";
  }
  return "false";
});

export const orgUnitVersionIdState = createSelector(_orgUnitVersionGetState, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});

export const orgUnitBulkDeleteDataState = createSelector(_orgUnitBulkDeleteSelect, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});

export const orgUnitListDataState = createSelector(_orgUnitListSelect, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});

export const dashboardCheckOUListDataState = createSelector(_orgUnitListSelect, data =>
  get(data, [DASHBOARD_OU_CHECK_LIST_ID], {})
);

export const orgUnitListRecordsState = createSelector(orgUnitListDataState, (data: any) => {
  return get(data, ["data", "records"], []);
});

export const getOrgUnitUtility = createSelector(orgUnitUtilityState, getUtility, (data: any, utility: string) => {
  const defaultValue = utility === "loading" ? "true" : [];
  return get(data, [utility], defaultValue);
});

const _ouProdScoreState = createSelector(restapiState, data =>
  get(data, ["organization_unit_productivity_score"], { loading: true, error: false })
);

const _ouProdScoreMethodState = createSelector(_ouProdScoreState, data =>
  get(data, ["list"], { loading: true, error: false })
);

export const _ouProdScoreSelect = createSelector(_ouProdScoreMethodState, getId, (data, id) =>
  get(data, [id], { loading: true, error: false })
);

const _orgUnitsForIntegrationSelect = createSelector(restapiState, (data: any) => {
  return get(data, ["org_units_for_integration"], { loading: true, error: false });
});

const _orgUnitsForIntegrationGetMethodSelect = createSelector(_orgUnitsForIntegrationSelect, (data: any) => {
  return get(data, ["get"], {});
});

export const orgUnitsForIntegrationGetDataSelect = createSelector(
  _orgUnitsForIntegrationGetMethodSelect,
  getId,
  (data: any, id: string) => {
    return get(data, [id], { loading: true, error: false });
  }
);

export const getSelectedOU = createSelector(restapiState, (data: any) => {
  return get(data, "selected-OU", undefined);
});
