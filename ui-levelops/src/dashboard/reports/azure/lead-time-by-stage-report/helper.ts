import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import DrilldownViewMissingCheckbox from "dashboard/reports/jira/lead-time-by-stage-report/DrilldownViewMissingCheckbox";
import { forEach, get, set } from "lodash";
import { Widget } from "model/entities/Widget";
import { AZURE_PARTIAL_FILTER_KEY_MAPPING } from "../constant";

export const getDrilldownCheckBox = () => {
  return DrilldownViewMissingCheckbox;
};

export const azureCommonPrevQueryTansformer = (widget: Widget) => {
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
  if(!query.hasOwnProperty("ratings")){
    set(query, ["ratings"], [ "good", "slow", "needs_attention" ]);
  }
  return widget;
};