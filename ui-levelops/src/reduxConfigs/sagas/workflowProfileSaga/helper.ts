import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { forEach } from "lodash";

//Will help to add metadta field in the Azure custom Filters
export const getUpdatedFilters = (filters: any, fieldList: any[]) => {
  forEach(filters, (filter: any) => {
    if (!filter?.metadata) {
      const fieldListCustom: IntegrationTransformedCFTypes | undefined = (fieldList ?? []).find(
        (field: IntegrationTransformedCFTypes) => field?.field_key === filter.key
      );
      if (fieldListCustom) filter.metadata = fieldListCustom?.metadata;
    }
  });
  return filters;
};
