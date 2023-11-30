import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";

export const buildDataForFilterApiCalling = (selectedReport: string, filters: any) => {
  const supportedFilters = get(widgetConstants, [selectedReport, "supported_filters"], undefined);
  if (supportedFilters && Array.isArray(supportedFilters)) {
    let apiCallData: { [x: string]: any[] } = {};
    supportedFilters.forEach((filter: DynamicGraphFilter) => {
      if (filter.filterType.includes("api") && filter.filterField !== "reporters") {
        if (get(filters, [filter.filterField], []).length > 0) {
          if (filter.filterField === "questionnaire_template_id") {
            apiCallData = {
              ...apiCallData,
              questionnaire_template_ids: get(filters, [filter.filterField], [])
            };
          }
          if (filter.filterField === "tags") {
            apiCallData = {
              ...apiCallData,
              tag_ids: get(filters, [filter.filterField], [])
            };
          }
          if (filter.filterField === "product_ids") {
            apiCallData = {
              ...apiCallData,
              product_ids: get(filters, [filter.filterField], [])
            };
          }

          if (filter.filterField === "assignees") {
            apiCallData = {
              ...apiCallData,
              user_ids: get(filters, [filter.filterField], [])
            };
          }

          if (filter.filterField === "states") {
            apiCallData = {
              ...apiCallData,
              state_ids: get(filters, [filter.filterField], [])
            };
          }
        }
      }
    });
    return apiCallData;
  }
  return [];
};

export const buildSpecificDataForFilterApiCalling = (filters: any, filterField: any) => {
  let apiCallData: any = {};
  if (get(filters, [filterField], []).length > 0) {
    if (filterField === "questionnaire_template_id") {
      apiCallData = {
        ...apiCallData,
        questionnaire_template_ids: get(filters, [filterField], [])
      };
    }
    if (filterField === "tags") {
      apiCallData = {
        ...apiCallData,
        tag_ids: get(filters, [filterField], [])
      };
    }
    if (filterField === "product_ids") {
      apiCallData = {
        ...apiCallData,
        product_ids: get(filters, [filterField], [])
      };
    }

    if (filterField === "assignees") {
      apiCallData = {
        ...apiCallData,
        user_ids: get(filters, [filterField], [])
      };
    }

    if (filterField === "states") {
      apiCallData = {
        ...apiCallData,
        state_ids: get(filters, [filterField], [])
      };
    }
  }
  return apiCallData;
};
