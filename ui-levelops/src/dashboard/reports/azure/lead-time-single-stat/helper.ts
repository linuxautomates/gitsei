import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { forEach, get, set, unset } from "lodash";
import { AZURE_PARTIAL_FILTER_KEY_MAPPING } from "../constant";

export const prevQueryTransformer = (widget: { query: any; type: string }) => {
  const { query } = widget;
  const partialKey = getWidgetConstant(widget?.type, PARTIAL_FILTER_KEY, "partial_match");
  const appliedPartialFilters = get(query, [partialKey], {});
  let newPartialMatchFilter: any = {};
  forEach(Object.keys(appliedPartialFilters), key => {
    newPartialMatchFilter = {
      ...newPartialMatchFilter,
      [get(AZURE_PARTIAL_FILTER_KEY_MAPPING, [key], key)]: get(appliedPartialFilters, [key])
    };
  });
  set(query, [partialKey], newPartialMatchFilter);
  if (!query?.calculation) {
    set(query, ["calculation"], "ticket_velocity");
  }
  if (query?.hasOwnProperty("limit_to_only_applicable_data")) {
    unset(query, "limit_to_only_applicable_data");
  }
  return widget;
};
