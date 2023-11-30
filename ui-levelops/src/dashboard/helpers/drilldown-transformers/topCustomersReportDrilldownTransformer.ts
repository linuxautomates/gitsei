import { valuesToFilters } from "dashboard/constants/constants";
import { get, unset } from "lodash";
import { genericDrilldownTransformer } from "./genericDrilldownTransformer";

export const topCustomersReportDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const filterValue = get(valuesToFilters, [acrossValue], acrossValue);
  const { widget } = data;
  const widgetFilter = widget.query || {};

  filters = {
    ...filters,
    filter: {
      ...filters.filter,
      [filterValue]: (widgetFilter[filterValue] || []).filter((key: string) => key !== acrossValue)
    }
  };

  if (get(filters, ["filter", filterValue], []).length === 0) {
    unset(filters, ["filter", filterValue]);
  }
  return { acrossValue, filters };
};
