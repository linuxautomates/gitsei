import { get, unset } from "lodash";
import { trimPartialStringFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import {
  PARTIAL_FILTER_TRANSFORM_KEY,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
} from "./filter-key.mapping";
import { getWidgetConstant } from "./widgetConstants";
import { sanitizePartialStringFilters } from "utils/filtersUtils";

export const mapPartialStringFilters = (filters: any, report: string) => {
  let finalFilters = filters;

  const partialFilterKey = getWidgetConstant(report, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);

  const partialFilterTransformer = getWidgetConstant(report, PARTIAL_FILTER_TRANSFORM_KEY);

  const partialStringFilters = get(finalFilters, ["filter", partialFilterKey], undefined);

  if (partialStringFilters) {
    const sanitizedPartialFilters = sanitizePartialStringFilters(partialStringFilters);
    if (Object.keys(sanitizedPartialFilters).length > 0) {
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters.filter || {}),
          [partialFilterKey]: sanitizedPartialFilters
        }
      };

      finalFilters = {
        ...(finalFilters || {}),
        filter: { ...(trimPartialStringFilters(finalFilters.filter) || {}) }
      };

      if (partialFilterTransformer) {
        const mappedFilters = partialFilterTransformer(finalFilters.filter, partialFilterKey);

        finalFilters = {
          ...(finalFilters || {}),
          filter: mappedFilters
        };
      }
    } else {
      unset(finalFilters, ["filter", partialFilterKey]);
    }
  }

  return finalFilters;
};
