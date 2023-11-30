import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { filtersTransformationUtilityType, filtersTransformerType } from "dashboard/dashboard-types/Dashboard.types";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import { cloneDeep } from "lodash";
import {
  convertChildKeysToSiblingKeys,
  updateTimeFiltersValue
} from "shared-resources/containers/widget-api-wrapper/helper";
import { sanitizeObject } from "utils/commonUtils";
import { NO_LONGER_SUPPORTED_FILTER } from "../applications/names";
import { getWidgetConstant } from "../widgetConstants";
import { mapPartialStringFilters } from "../mapPartialFilters.helper";

/**
 * Use this generic filters transformer for intial transformer
 */
export const genericCSVFiltersTransformer: filtersTransformerType = (
  filters: basicMappingType<any>,
  utilityConfig: filtersTransformationUtilityType
) => {
  let finalFilters: basicMappingType<any> = cloneDeep(filters);
  const { widgetMetadata, dashboardMetadata, uri, report } = utilityConfig;

  /** bringing some filters up the hirarchy */
  finalFilters = convertChildKeysToSiblingKeys(finalFilters, "filter", [
    "across",
    "stacks",
    "sort",
    "interval",
    "filter_across_values"
  ]);

  /** Mapping partial filters */
  finalFilters = mapPartialStringFilters(finalFilters, report);

  /** Handling time filters */
  finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, widgetMetadata ?? {}, report ?? "", uri ?? "");

  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...(finalFilters?.filter ?? {}),
      ...updateTimeFiltersValue(dashboardMetadata, widgetMetadata, { ...finalFilters?.filter })
    }
  };

  /**
   *  Removing no longer supported filters
   */
  const removeNoLongerSupportedFilter = getWidgetConstant(report, [NO_LONGER_SUPPORTED_FILTER], undefined);
  if (removeNoLongerSupportedFilter) {
    finalFilters = {
      ...finalFilters,
      filter: { ...removeNoLongerSupportedFilter(finalFilters.filter) }
    };
  }

  /** Sanitizing for removing empty/falsy fields */
  finalFilters = sanitizeObject({
    ...(finalFilters || {}),
    filter: sanitizeObject(finalFilters?.filter || {})
  });

  return finalFilters;
};
