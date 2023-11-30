import { get, unset } from "lodash";
import { DRILLDOWN_SUPPORTED_FILTERS_KEYS, NOT_SUPPORTED_FILTERS } from "./constant";

export const onChartClickPayload = (params: { [key: string]: any }) => {
  const { workitem_id } = params;
  const featureIntegrationId = get(params, "integration_id", 0);
  return { id: workitem_id, integration_ids: [featureIntegrationId] };
};

export const removeHiddenFiltersFromPreview = (params: any) => {
  const filter = get(params, "filter", {});
  const filterKeys = Object.keys(filter).forEach((key: string) => {
    if (["across", "workitem_parent_workitem_types"].includes(key)) {
      unset(filter, key);
    }
  });
  return filter;
};

export const drilldownTransformFunction = (params: any) => {
  const filters = get(params, "filter", {});
  const key = get(params, "key", "");
  const data = get(params, "onClickData", {});
  const finalFilters = {
    ...filters,
    filter: { ...filters?.filter, [key]: [data?.id], integration_ids: data?.integration_ids }
  };
  const filterkeys = Object.keys(finalFilters?.filter);
  filterkeys.forEach((key: string) => {
    if (!DRILLDOWN_SUPPORTED_FILTERS_KEYS.includes(key)) {
      unset(finalFilters, ["filter", key]);
    }
  });
  return finalFilters;
};
/**
 * Returns new filters object with by removing unsupported filters.
 *
 * @param  { Object } filter all filters of widget.
 * @return { Object } filter object.
 */
export const removeNoLongerSupportedFilters = (filter: any) => {
  let newfilter = { ...(filter || {}) };
  NOT_SUPPORTED_FILTERS.forEach((item: string) => {
    unset(newfilter, [item]);
  });
  return newfilter;
};
