import { mapAzureCustomHygieneFilters, mapAzureMissingFields } from "configurations/helpers/map-filters.helper";
import { get } from "lodash";

type custom_hygiene = {
  selectedFields: Array<any>;
};

export const getTransformedAzureCustomHygienes = (custom_hygienes: any, tranformedCustomFieldRecords: any) => {
  return custom_hygienes.map((props: custom_hygiene) => {
    const { selectedFields, ...rest } = props;
    return {
      ...rest,
      missing_fields: mapAzureMissingFields((rest as any)?.missing_fields, tranformedCustomFieldRecords),
      filter: mapAzureCustomHygieneFilters(get(rest, ["filter"], {}), tranformedCustomFieldRecords)
    };
  });
};
