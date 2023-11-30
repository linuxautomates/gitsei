import { RestWidget } from "classes/RestDashboards";
import { TableFiltersBEKeys } from "configurable-dashboard/components/configure-widget/configuration/table/constant";
import { get } from "lodash";

/**
 * If the tableId is not present in the payload, or if the showOUData is true and the
 * ou_id_column_exists is false, then return false
 * @param {RestWidget} payload - RestWidget - this is the payload that is sent to the backend.
 * @returns A function that takes a payload and returns a boolean.
 */
export const tableReportValidatorFunction = (payload: RestWidget) => {
  const tableId = payload?.tableId;
  const { ou_id_column_exists } = payload?.metadata ?? {};
  const showOUData = get(payload?.query, [TableFiltersBEKeys.SHOW_VALUES_OF_SELECTED_OU], false);
  if (!tableId || (showOUData && !ou_id_column_exists)) return false;
  return true;
};
