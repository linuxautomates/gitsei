import { get } from "lodash";

export const getAcrossValue = (args: any) => {
  const filters = args?.allFilters;
  let across = filters?.across;
  if (["ticket_created"].includes(across)) {
    const interval = get(filters, ["interval"]);
    if (interval) across = `${across}_${interval}`;
  }
  return across;
};

export const getZendeskTicketsReportSortKey = (params: any) => {
  const { across } = params;
  let key = undefined;
  if (["ticket_created"].includes(across)) {
    key = "key";
  }

  return key;
};

export const getZendeskTicketsReportSortOrder = (params: any) => {
  const { interval, across } = params;
  let key = undefined;
  if (["ticket_created"].includes(across)) {
    if (["month"].includes(interval)) {
      key = "asc";
    }
  }

  return key;
};
