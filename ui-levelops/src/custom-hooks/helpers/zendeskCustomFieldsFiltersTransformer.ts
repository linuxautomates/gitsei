import { Dict } from "types/dict";
import { get, forEach, cloneDeep, unset } from "lodash";
import { sanitizeFilters } from "utils/filtersUtils";
import { CUSTOM_FIELD_PREFIX, CUSTOM_FIELD_STACK_FLAG } from "dashboard/constants/constants";

export const zendeskCustomFieldFiltersKeyMappingTransform = (filters: any, mapping: Dict<string, string>) => {
  const firstLevelKeys = Object.keys(filters);
  let secondLevelKeys: any = [];
  if (get(filters, ["filter"], undefined)) {
    secondLevelKeys = Object.keys(get(filters, ["filter"], undefined));
  }

  let newFilters = cloneDeep(filters);

  forEach(Object.keys(mapping), (key: string) => {
    const newKey = get(mapping, [key], key);
    if (firstLevelKeys.includes(key)) {
      newFilters[newKey] = filters[key];
      unset(newFilters, [key]);
    } else if (secondLevelKeys.includes(key)) {
      if (key !== "exclude") {
        newFilters = {
          ...(newFilters || {}),
          filter: {
            ...get(newFilters, ["filter"], []),
            [newKey]: get(filters, ["filter", key], undefined)
          }
        };
        unset(newFilters, ["filter", key]);
      } else {
        const exCustomFields = get(filters, ["filter", "exclude", "custom_fields"], {});
        newFilters = {
          ...(newFilters || {}),
          filter: {
            ...get(newFilters, ["filter"], []),
            [newKey]: exCustomFields
          }
        };
        unset(newFilters, ["filter", "exclude", "custom_fields"]);
      }
    }
  });

  return sanitizeFilters(newFilters);
};

export const customFieldFiltersSanitize = (filters: any, isWidget: boolean) => {
  const firstLevelKeys = Object.keys(filters);
  let secondLevelKeys: any = [];
  let secondLevelFilters: any = {};

  if (get(filters, ["filter"], undefined)) {
    secondLevelKeys = Object.keys(get(filters, ["filter"], undefined));
    secondLevelFilters = get(filters, ["filter"], undefined);
  }
  const newFilters = cloneDeep(filters);

  forEach(firstLevelKeys, key => {
    const value = filters[key];
    if (typeof value === "string") {
      if (value.includes(CUSTOM_FIELD_PREFIX)) {
        if (key === "across") {
          if (isWidget) {
            newFilters.filter["custom_across"] = value.slice(CUSTOM_FIELD_PREFIX.length);
            newFilters[key] = CUSTOM_FIELD_STACK_FLAG;
          } else {
            // Reports and dashboard drilldowns don't require across.
          }
        } else {
          newFilters[key] = value.slice(CUSTOM_FIELD_PREFIX.length);
        }
      }
    }
  });

  forEach(secondLevelKeys, key => {
    switch (key) {
      case "stacks":
      case "custom_stacks":
        newFilters.filter[key] = secondLevelFilters?.[key].map((value: string) => {
          if (value.includes(CUSTOM_FIELD_PREFIX)) {
            return value.slice(CUSTOM_FIELD_PREFIX.length);
          }
          return value;
        });
        break;
      case "custom_fields":
        const customFields = secondLevelFilters?.[key];
        const res = Object.keys(customFields).reduce((acc: any, ckey: string) => {
          if (ckey.includes(CUSTOM_FIELD_PREFIX)) {
            return {
              ...acc,
              [ckey.slice(CUSTOM_FIELD_PREFIX.length)]: customFields?.[ckey]
            };
          }
          return {
            ...acc,
            [ckey]: customFields?.[ckey]
          };
        }, {});
        newFilters.filter[key] = res;
        break;
      case "exclude":
        const exCustomFields = get(secondLevelFilters, ["exclude", "custom_fields"], {});
        const excludedData = get(secondLevelFilters, ["exclude"], {});
        if (Object.keys(exCustomFields).length > 0) {
          newFilters.filter[key] = {
            ...excludedData,
            custom_fields: Object.keys(exCustomFields).reduce((acc: any, ckey: string) => {
              if (ckey.includes(CUSTOM_FIELD_PREFIX)) {
                return {
                  ...acc,
                  [ckey.slice(CUSTOM_FIELD_PREFIX.length)]: exCustomFields?.[ckey]
                };
              }
              return {
                ...acc,
                [ckey]: exCustomFields?.[ckey]
              };
            }, {})
          };
        }
    }

    if (key.includes(CUSTOM_FIELD_PREFIX)) {
      const value = newFilters.filter[key];
      if (!newFilters.filter.custom_fields) {
        newFilters.filter.custom_fields = {};
      }
      newFilters.filter.custom_fields = {
        ...newFilters.filter.custom_fields,
        [key.slice(CUSTOM_FIELD_PREFIX.length)]: value
      };
    }
  });
  return sanitizeFilters(newFilters);
};
