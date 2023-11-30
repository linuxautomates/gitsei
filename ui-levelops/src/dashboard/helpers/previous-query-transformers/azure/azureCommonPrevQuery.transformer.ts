import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { AZURE_PARTIAL_FILTER_KEY_MAPPING } from "dashboard/reports/azure/constant";
import { forEach, get, set } from "lodash";
import { Widget } from "model/entities/Widget";

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

  const azureIterationFilters = get(query, ["azure_iteration"], []);
  if (azureIterationFilters && azureIterationFilters.length) {
    const newAzureIterationFilter = azureIterationFilters.map((item: string | { parent: string; child: string }) => {
      if (typeof item === "string") {
        return item;
      }
      return `${item.parent}\\${item.child}`;
    });
    set(query, ["azure_iteration"], newAzureIterationFilter);
  }
  return widget;
};
