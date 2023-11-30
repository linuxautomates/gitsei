import { RestDevProdEngineer } from "classes/RestDevProdEngineer";
import { DEV_PRODUCTIVITY_USER_PR_ACTIVITY } from "dashboard/pages/scorecard/components/PRActivity/helpers";
import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getId = createParameterSelector((param: any) => param.id);

// **************** Engineer snapshot and scorecard selectors *****************

const _devProdEngineerState = createSelector(restapiState, data =>
  get(data, ["dev_productivity_reports"], { loading: true, error: false })
);

const _devProdEngineerMethodState = createSelector(_devProdEngineerState, data =>
  get(data, ["list"], { loading: true, error: false })
);

const _devProdEngineerUniqueSelect = createSelector(_devProdEngineerMethodState, getId, (data, id) =>
  get(data, [id], { loading: true, error: false })
);

export const devProductivityReportLoading = createSelector(_devProdEngineerUniqueSelect, (data: any) =>
  get(data, ["loading"], true)
);

export const devProductivityReportError = createSelector(_devProdEngineerUniqueSelect, (data: any) =>
  get(data, ["error"], false)
);

export const devProdEngineerSelect = createSelector(_devProdEngineerUniqueSelect, (data: any) =>
  get(data, ["data"], [])
);

export const devProRestEngineerSelect = createSelector(devProdEngineerSelect, data => {
  if (!!data) return new RestDevProdEngineer(data);
  return {};
});

const _devProdEngineerSnapshotMethodState = createSelector(_devProdEngineerState, data =>
  get(data, ["get"], { loading: true, error: false })
);

const _devProdEngineerSnapshotUniqueSelect = createSelector(_devProdEngineerSnapshotMethodState, getId, (data, id) =>
  get(data, [id], { loading: true, error: false })
);

export const devProdEngineerSnapshotSelect = createSelector(_devProdEngineerSnapshotUniqueSelect, (data: any) =>
  get(data, ["data"], {})
);

export const devProductivityEngineerSnapshotLoading = createSelector(
  _devProdEngineerSnapshotUniqueSelect,
  (data: any) => get(data, ["loading"], true)
);

// **************** Org scorecard selctors *****************

const _devProdOrgUnitState = createSelector(restapiState, data =>
  get(data, ["dev_productivity_org_unit_score_report"], { loading: true, error: false })
);

const _devProdOrgUnitMethodState = createSelector(_devProdOrgUnitState, data =>
  get(data, ["list"], { loading: true, error: false })
);

const _devProdOrgUnitUniqueSelect = createSelector(_devProdOrgUnitMethodState, getId, (data, id) =>
  get(data, [id], { loading: true, error: false })
);

export const devProdOrgUnitSelect = createSelector(_devProdOrgUnitUniqueSelect, (data: any) =>
  get(data, ["data", "records"], [])
);

// **************** Relative score selectors*****************

const _devProdRelativeScoreState = createSelector(restapiState, data =>
  get(data, ["dev_productivity_relative_score"], { loading: true, error: false })
);

const _devProdRelativeScoreMethodState = createSelector(_devProdRelativeScoreState, data =>
  get(data, ["list"], { loading: true, error: false })
);

export const _devProdRelativeScoreSelect = createSelector(_devProdRelativeScoreMethodState, getId, (data, id) =>
  get(data, [id], { loading: true, error: false })
);

const _devProdPRActivityState = createSelector(restapiState, data =>
  get(data, [DEV_PRODUCTIVITY_USER_PR_ACTIVITY, "list"], { loading: true, error: false })
);

export const devProdPRActivityLoading = createSelector(_devProdPRActivityState, getId, (data: any, id: string) =>
  get(data, [id, "loading"], true)
);

export const devProdPRActivityData = createSelector(_devProdPRActivityState, getId, (data: any, id: string) =>
  get(data, [id, "data"], undefined)
);

export const devProdPRActivityError = createSelector(_devProdPRActivityState, getId, (data: any, id: string) =>
  get(data, [id, "error"], false)
);
