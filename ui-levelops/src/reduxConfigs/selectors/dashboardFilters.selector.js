import { createSelector } from "reselect";
import { get } from "lodash";

const filtersRestState = state => {
  const def = { get: {}, list: {}, create: {}, delete: {}, update: {} };
  const products = get(state.restapiReducer, ["products"], def);
  const mappings = get(state.restapiReducer, ["mappings"], def);
  const integrations = get(state.restapiReducer, ["integrations"], def);
  return {
    products: products,
    mappings: mappings,
    integrations: integrations
  };
};

export const getDashboardFiltersSelector = createSelector(filtersRestState, data => {
  return data;
});

export const dashboardReportsDeleteState = state => {
  const delState = get(state.restapiReducer, ["dashboard_reports", "delete"], {});
  return { dashboard_reports_delete: delState };
};
