import { forEach, get, set, unset } from "lodash";
import moment from "moment";
import { genericDrilldownTransformer } from ".";

export const levelopsTableReportDrilldownTransformer = (data: any) => {
  const { filters } = genericDrilldownTransformer(data);
  const { drillDownProps, metaData, queryParamOU, dashboardMetadata } = data;
  let { x_axis } = drillDownProps;
  const xAxisFilterKey = get(metaData, ["xAxis"], "");
  const xAxisType = get(metaData, ["xAxisType"], "");
  if (xAxisType && xAxisType === "date") {
    const dateTimeStamp = moment.utc(x_axis, "MM/DD/YYYY");
    const gt = dateTimeStamp.startOf("D").unix().toString();
    const lt = dateTimeStamp.endOf("D").unix().toString();
    x_axis = { $gt: gt, $lt: lt };
  }
  const ou_id = queryParamOU ? [queryParamOU] : get(dashboardMetadata, "ou_ids", []);
  const finalFilters = {
    ...(filters ?? {}),
    filter: {
      ...get(filters, ["filter"], {}),
      [xAxisFilterKey]: x_axis
    }
  };
  if (ou_id?.length) {
    set(finalFilters, ["filter", "ou_id"], ou_id);
  }
  const keysToUnset: string[] = ["integration_ids", "product_id"];
  forEach(keysToUnset, key => {
    unset(finalFilters, ["filter", key]);
  });
  return { acrossValue: xAxisFilterKey, filters: finalFilters };
};
