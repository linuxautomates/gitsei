import { cloneDeep, get, set, uniq, unset } from "lodash";

/** This is a common azure widget filters transformer
 *  add this to the report config under the key "widget_filter_transform"
 *  and use this to transform the payload filters for drilldown and widget
 * @param filters
 * @returns transformed filters
 */
export function azureCommonFilterTransformFunc<T extends string | Array<string> | object>(
  filters: Record<string, T>
): Record<string, T> {
  let allFilters = cloneDeep(filters);
  let nfilters = get(allFilters, ["filter"], {});
  const filterKeys: string[] = Object.keys(nfilters);
  if (filterKeys.includes("workitem_feature") || filterKeys.includes("workitem_user_story")) {
    const features: string[] = get(nfilters, ["workitem_feature"], []);
    const userStories: string[] = get(nfilters, ["workitem_user_story"], []);
    const prevParentWorkitemIds: string[] = get(nfilters, ["workitem_parent_workitem_ids"], []);
    set(
      nfilters as object,
      ["workitem_parent_workitem_ids"],
      uniq([...features, ...userStories, ...prevParentWorkitemIds])
    );
    unset(nfilters, ["workitem_feature"]);
    unset(nfilters, ["workitem_user_story"]);
  }
  set(allFilters, ["filter"], nfilters);
  return allFilters;
}
