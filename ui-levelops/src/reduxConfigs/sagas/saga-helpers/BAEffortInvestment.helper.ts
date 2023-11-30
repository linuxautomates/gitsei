import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { RestTicketCategorizationProfileJSONType } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { cloneDeep, forEach, get, set } from "lodash";

export const getLeastTimeBound = (filters: { [key: string]: any }) => {
  const timeFilters: { $gt: string | number; $lt: string | number }[] = [];

  Object.keys(filters).forEach(key => {
    if (Object.keys(filters[key]) && filters[key].hasOwnProperty("$gt") && filters[key].hasOwnProperty("$lt")) {
      timeFilters.push(filters[key]);
    }
  });

  const allGTtimes = timeFilters.map(item => parseInt(item["$gt"].toString()));
  const allLTtimes = timeFilters.map(item => parseInt(item["$lt"].toString()));

  return {
    max: Math.min(...allLTtimes),
    min: Math.max(...allGTtimes)
  };
};

export const updateEffortInvestmentProfile = (
  profile: RestTicketCategorizationProfileJSONType,
  customFieldRecords: IntegrationTransformedCFTypes[]
) => {
  const clonnedProfile = cloneDeep(profile);
  const categories = clonnedProfile?.config?.categories ?? {};

  if (Object.keys(categories).length) {
    let newCategories: { [x: string]: any }[] = [];
    forEach(Object.values(categories), (category, index) => {
      const filters = get(category, ["filter"], {});
      if (filters.hasOwnProperty("workitem_custom_fields")) {
        const customFieldKeys = Object.keys(filters.workitem_custom_fields);
        const customFilter = customFieldKeys.reduce((acc: any, next: string) => {
          const config = customFieldRecords.find((item: any) => item.field_key === next);
          if (config?.hasOwnProperty("metadata") && config?.metadata?.transformed) {
            const key = next.replace("Custom.", "");
            return { ...acc, [key]: filters.workitem_custom_fields[next] };
          }
          return { ...acc, [next]: filters.workitem_custom_fields[next] };
        }, {});

        const newFilters = {
          ...filters,
          workitem_custom_fields: customFilter
        };
        newCategories.push({
          ...(category ?? {}),
          filter: newFilters
        });
      } else {
        newCategories.push(category);
      }
    });

    const transformedNewCategories = newCategories.reduce((acc, category) => {
      if (category.index !== undefined) {
        acc = { ...acc, [category.index?.toString()]: category };
      }
      return acc;
    }, {});

    set(clonnedProfile, ["config", "categories"], transformedNewCategories);
  }
  return clonnedProfile;
};
