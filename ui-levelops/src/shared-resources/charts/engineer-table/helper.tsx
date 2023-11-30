import React from "react";
import { capitalize, get } from "lodash";
import { basicMappingType } from "../../../dashboard/dashboard-types/common-types";
import { RestWidget } from "../../../classes/RestDashboards";
import {
  azureActiveWorkUnitOptions,
  azureEffortInvestmentUnitFilterOptions,
  jiraActiveWorkUnitOptions,
  ticketCategorizationUnitFilterOptions
} from "../../../dashboard/constants/bussiness-alignment-applications/constants";
import { AllocationSummaryDataContent } from "./constants";
const TICKET_TIME_SPENT = "Ticket Time Spent";

export const getIntegerValue = (value: string | number = "", type: "total" | "percentage" = "total") => {
  const valueArray = value.toString().split("|");
  return type === "total"
    ? parseFloat((valueArray[0] || "0").trim())
    : parseFloat((valueArray[1] || "0").trim()).toFixed(2);
};

export const getUriUnit = (widget: RestWidget) => {
  const isCompletedEffortActive = widget?.metadata?.effort_type === "COMPLETED_EFFORT";
  const unit = isCompletedEffortActive ? widget?.query?.uri_unit : widget?.query?.active_work_unit;
  const filterOptions = isCompletedEffortActive
    ? [...ticketCategorizationUnitFilterOptions, ...azureEffortInvestmentUnitFilterOptions]
    : [...jiraActiveWorkUnitOptions, ...azureActiveWorkUnitOptions];
  return filterOptions.find(_option => _option?.value === unit)?.label;
};

export const isTotalInDays = (uriUnit: any) => uriUnit === TICKET_TIME_SPENT;

export const getAllocationSummarySubColumn = (
  category: string,
  uriUnit: any,
  index = 0,
  type: "total" | "percentage" = "total"
) => ({
  title: `${capitalize(type)} ${type === "total" && isTotalInDays(uriUnit) ? "(days)" : ""}`.trim(),
  dataIndex: "allocation_summary",
  sorter: (mapping1: basicMappingType<number>, mapping2: basicMappingType<number>) => {
    const value1 = get(mapping1 || {}, ["allocation_summary", category], 0);
    const value2 = get(mapping2 || {}, ["allocation_summary", category], 0);
    // @ts-ignore
    return getIntegerValue(value1, type) - getIntegerValue(value2, type);
  },
  sortDirections: ["descend", "ascend"],
  key: `${type}_${index}_${category}`,
  width: isTotalInDays(uriUnit) ? 140 : 120,
  className: "category-header",
  render: (item: basicMappingType<number>) => {
    return (
      <div className="effort-score-text">
        {getIntegerValue(item?.[category], type) ?? "0.00"}
        {type === "percentage" && "%"}
      </div>
    );
  }
});

export const engineerTableSortingHelper = (
  a: any,
  b: any,
  order: any,
  targetColumnLabel: string,
  subColumn: string
) => {
  const targetDataPoint1 = get(a, ["allocation_summary", targetColumnLabel], "").split("|");
  const targetDataPoint2 = get(b, ["allocation_summary", targetColumnLabel], "").split("|");
  // @ts-ignore
  let v1 = targetDataPoint1?.[AllocationSummaryDataContent[subColumn]];
  // @ts-ignore
  let v2 = targetDataPoint2?.[AllocationSummaryDataContent[subColumn]];
  v1 = !!v1 ? v1 : "0";
  v2 = !!v2 ? v2 : "0";
  return order === "ascend" ? +v1 - +v2 : +v2 - +v1;
};

export const engineerTableGenericSortingHelper = (
  mapping1: basicMappingType<number>,
  mapping2: basicMappingType<number>,
  category: string
) => {
  const value1 = get(mapping1 || {}, ["allocation_summary", category], 0);
  const value2 = get(mapping2 || {}, ["allocation_summary", category], 0);
  return value1 - value2;
};
