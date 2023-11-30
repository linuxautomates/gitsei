import { get, set, unset } from "lodash";
import { Widget } from "model/entities/Widget";
import { azureCommonPrevQueryTansformer } from "./azureCommonPrevQuery.transformer";

export const azureLeadTimeTrendPrevQueryTansformer = (widget: Widget): Widget => {
  const nwidget: Widget = azureCommonPrevQueryTansformer(widget);
  const { query } = nwidget;
  const metrics = get(query, ["metrics"]);
  if (metrics) {
    set(nwidget, ["metadata", "metrics"], metrics);
    unset(nwidget, ["query", "metrics"]);
  }
  return nwidget;
};
