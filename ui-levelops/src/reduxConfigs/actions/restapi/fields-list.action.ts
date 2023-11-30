import { LOAD_DASHBOARD_FIELDS_LIST } from "../actionTypes";

export const FIELD_LIST_ENTITY = "selected-dashboard-field-list-data";

export const loadSelectedDashboardFieldsList = () => ({
  type: LOAD_DASHBOARD_FIELDS_LIST
});
