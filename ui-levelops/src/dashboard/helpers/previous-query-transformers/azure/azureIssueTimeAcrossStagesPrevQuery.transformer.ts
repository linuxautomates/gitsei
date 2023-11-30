import { cloneDeep, get, set, unset } from "lodash";
import { Widget } from "model/entities/Widget";
import { azureCommonPrevQueryTansformer } from "./azureCommonPrevQuery.transformer";

/**
 * It takes a widget object, clones the query object, removes the metrics property from the cloned
 * query object, adds a metric property to the cloned query object, and then sets the widget's query
 * property to the cloned query object
 * @param {Widget} widget - The widget object that is being transformed.
 * @returns A function that takes a widget as an argument and returns a widget.
 */
export function azureTimeAcrossStagesPrevQueryTansformer(widget: Widget) {
  const nwidget = azureCommonPrevQueryTansformer(widget);
  const { query } = nwidget;
  const clonedQuery = cloneDeep(query);
  const metricValue = get(clonedQuery, ["metrics"]);
  if (!!metricValue) {
    unset(clonedQuery, ["metrics"]);
    set(clonedQuery, ["metric"], metricValue);
  }
  set(nwidget, ["query"], clonedQuery);
  return nwidget;
}
