import widgetConstants from "dashboard/constants/widgetConstants";
import { get, unset } from "lodash";
import moment from "moment";
import { getCurrentTimezoneOffset } from "utils/timeUtils";
import { levelOpsFiltersMap } from "../../../configurable-dashboard/dynamic-graph-filter/container/constants";
import { levelopsAcrossMap } from "../../constants/helper";
import { DateFormats } from "./../../../utils/dateUtils";
import { LEVELOPS_REPORTS } from "../../../dashboard/reports/levelops/constant";

export const levelopsDrilldownTransformer = (data: any) => {
  const { drillDownProps, widget, dashboardQuery } = data;
  const { x_axis } = drillDownProps;
  let { across, ...remainData } = widget.query;
  const widgetFilter = get(widgetConstants, [widget.type, "filters"], {});
  if (widgetFilter.across) {
    across = widgetFilter.across;
  }

  let filterValue = get(levelopsAcrossMap, [across], across);
  let xAxisValue = x_axis !== " " ? x_axis : undefined;
  if (typeof x_axis === "object" && Object.keys(x_axis).length) {
    if (filterValue === "status") {
      xAxisValue = x_axis.name;
    } else {
      xAxisValue = x_axis.id;
    }
  }

  let acrossFilterValue: any = xAxisValue ? [xAxisValue.includes("UNASSIGNED") ? "_UNASSIGNED_" : xAxisValue] : [];
  if (["status", "reporters"].includes(filterValue)) {
    acrossFilterValue = xAxisValue;
  } else if (filterValue === "completed") {
    acrossFilterValue = xAxisValue === "true";
  }

  let widgetQuery = remainData;
  across = get(levelOpsFiltersMap, [across], across);
  let mappedFilters: any = {};
  Object.keys(widgetQuery).forEach(key => {
    const filterKey = get(levelopsAcrossMap, [key], key);
    mappedFilters = {
      ...mappedFilters,
      [filterKey]: widgetQuery[key]
    };
  });

  if (mappedFilters?.reporters && mappedFilters?.reporters.length > 0) {
    mappedFilters = {
      ...mappedFilters,
      reporters: mappedFilters?.reporters[0].label
    };
    unset(mappedFilters, ["reporters"]);
  }

  widgetQuery = mappedFilters;
  unset(dashboardQuery, ["product_id"]);
  unset(dashboardQuery, ["integration_ids"]);

  if (widget._type === LEVELOPS_REPORTS.WORKITEM_COUNT_REPORT && filterValue === "reporter") filterValue = "reporters";

  let filters = {
    filter: {
      ...(widgetQuery || {}),
      ...(dashboardQuery || {}),
      [filterValue]: acrossFilterValue
    },
    across
  };

  if (acrossFilterValue === "unknown") {
    unset(filters, ["filter", filterValue]);
  }

  const timeAcross: string[] = ["created", "updated", "trend"];
  if (timeAcross.includes(across)) {
    const timeAxisMap: Record<string, string> = { created: "created_at", updated: "updated_at", trend: "updated_at" };
    const selectedDate = typeof x_axis === "string" ? x_axis : x_axis?.name;
    let lt = moment(selectedDate, DateFormats.DAY_MONTH)
      .add(getCurrentTimezoneOffset(), "minutes")
      .utc()
      .endOf("d")
      .unix();
    if (across === "trend") {
      lt = moment(selectedDate, DateFormats.DAY_MONTH)
        .add(getCurrentTimezoneOffset(), "minutes")
        .utc()
        .startOf("d")
        .unix();
    }
    const gt = moment(selectedDate, DateFormats.DAY_MONTH)
      .add(getCurrentTimezoneOffset(), "minutes")
      .utc()
      .startOf("d")
      .unix();
    const timeFilterValue = { $lt: lt, $gt: across === "trend" ? 0 : gt };
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        [timeAxisMap[across]]: timeFilterValue
      }
    };
    unset(filters, ["filter", across]);
  }

  return { acrossValue: across, filters };
};

export const levelopsResponseTimeReportDrilldownTransformer = (data: any) => {
  const { drillDownProps } = data;
  const { x_axis } = drillDownProps;
  let { filters, acrossValue } = levelopsDrilldownTransformer(data);
  const filterValue = get(levelopsAcrossMap, [x_axis], x_axis);
  const { widget } = data;
  const widgetFilter = widget.query || {};

  filters.across = x_axis;
  filters = {
    ...filters,
    filter: {
      ...filters.filter,
      [filterValue]: (widgetFilter[filterValue] || []).filter((key: string) => key !== x_axis)
    }
  };

  if (get(filters, ["filter", filterValue], []).length === 0) {
    unset(filters, ["filter", filterValue]);
  }

  if (acrossValue === "assessment") {
    return { acrossValue: x_axis, filters };
  }

  return { acrossValue, filters };
};
