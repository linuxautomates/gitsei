import { get } from "lodash";
import { idFilters } from "../commonJiraReports.constants";

export const onChartClickPayload = (params: any) => {
  const { data, across } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (idFilters.includes(across)) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  }
  return keyValue;
};
