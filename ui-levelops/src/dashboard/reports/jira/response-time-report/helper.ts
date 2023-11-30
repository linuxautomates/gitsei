import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { get } from "lodash";
import { idFilters } from "../commonJiraReports.constants";

export const responseTimeOnChartClickPayloadHandler = (args: { data: basicMappingType<any>; across?: string }) => {
  const { data, across } = args;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (across && idFilters.includes(across)) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  }
  return keyValue;
};
