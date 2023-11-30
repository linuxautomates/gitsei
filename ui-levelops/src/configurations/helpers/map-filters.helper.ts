import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { AZURE_CUSTOM_FIELD_PREFIX, CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { cloneDeep, findKey, forEach, get, unset } from "lodash";
import { sanitizeObjectCompletely } from "utils/commonUtils";

export const mapJiraIntegrationFilters = (filters: any) => {
  let newFilters = filters;
  const customFields = Object.keys(filters || {}).reduce((acc: any, key: string) => {
    if (key.includes(CUSTOM_FIELD_PREFIX)) {
      acc = { ...acc, [key]: filters[key] };
    }
    return acc;
  }, {});
  const excludeCustomFields = Object.keys(filters.exclude || {}).reduce((acc: any, key: string) => {
    if (key.includes(CUSTOM_FIELD_PREFIX)) {
      acc = { ...acc, [key]: filters?.exclude?.[key] };
    }
    return acc;
  }, {});
  newFilters = {
    ...newFilters,
    custom_fields: customFields,
    exclude: { ...(newFilters.exclude || {}), custom_fields: excludeCustomFields }
  };
  forEach(Object.keys(newFilters || {}), key => {
    if (key.includes(CUSTOM_FIELD_PREFIX)) {
      unset(newFilters, [key]);
    }
  });
  forEach(Object.keys(newFilters.exclude || {}), key => {
    if (key.includes(CUSTOM_FIELD_PREFIX)) {
      unset(newFilters, ["exclude", key]);
    }
  });
  return newFilters;
};

export const mapAzureCustomHygieneFilters = (filters: any, transformedFields: IntegrationTransformedCFTypes[]) => {
  const newFilters = cloneDeep(filters);
  const excludeFilters = get(newFilters, ["exclude"], {});
  const fieldSizeFilters = get(newFilters, ["field_size"], {});
  unset(newFilters, ["exclude"]);
  unset(newFilters, ["field_size"]);
  const getReducedFilters = (nFilters: any) =>
    Object.keys(nFilters).reduce((acc, key) => {
      if (key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
        const transformedRecord = transformedFields?.find(f => f.field_key === key);
        let fieldKey = key;
        if (transformedRecord && transformedRecord?.metadata?.transformed) {
          fieldKey = key.replace(transformedRecord?.metadata?.transformed, "");
        }
        return { ...acc, [fieldKey]: nFilters[key] };
      }
      return { ...acc, [key]: nFilters[key] };
    }, {});
  const normalFilters = getReducedFilters(newFilters);
  const nExcludeFilters = getReducedFilters(excludeFilters);
  return sanitizeObjectCompletely({
    ...(normalFilters ?? {}),
    exclude: nExcludeFilters,
    field_size: getReducedFilters(fieldSizeFilters)
  });
};

export const mapAzureMissingFields = (
  missingFields: Record<string, boolean>,
  transformedFields: IntegrationTransformedCFTypes[]
) => {
  return Object.keys(missingFields || {}).reduce((acc: any, key: string) => {
    if (key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      const transformedRecord = transformedFields?.find(f => f.field_key === key);
      let fieldKey = key;
      if (transformedRecord && transformedRecord?.metadata?.transformed) {
        fieldKey = key.replace(transformedRecord?.metadata?.transformed, "");
      }
      return { ...acc, [fieldKey]: missingFields[key] };
    }
    return { ...acc, [key]: missingFields[key] };
  }, {});
};

export const transformAzureMissingAndFieldSizeFilters = (
  filters: Record<string, boolean>,
  transformedFields: IntegrationTransformedCFTypes[]
) => {
  return Object.keys(filters || {}).reduce((acc: any, key: string) => {
    const transformedRecord = transformedFields?.find(f => {
      if (f?.metadata?.transformed) {
        const nKey = f?.field_key?.replace(f?.metadata?.transformed, "");
        return nKey === key;
      }
    });
    let fieldKey = key;
    if (transformedRecord) {
      fieldKey = `${transformedRecord?.metadata?.transformed}${key}`;
    }
    return { ...acc, [fieldKey]: filters[key] };
  }, {});
};

export const mapAzureIntegrationFilters = (filters: any, transformedFields: IntegrationTransformedCFTypes[]) => {
  let newFilters = cloneDeep(filters);
  const getReducedCustomFieldFilters = (cfilters: any) =>
    Object.keys(cfilters || {}).reduce((acc: any, key: string) => {
      if (key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
        const transformedRecord = transformedFields?.find(f => f.field_key === key);
        let fieldKey = key;
        if (transformedRecord && transformedRecord?.metadata?.transformed) {
          fieldKey = key.replace(transformedRecord?.metadata?.transformed, "");
        }
        acc = { ...acc, [fieldKey]: cfilters[key] };
      }
      return acc;
    }, {});

  const customFields = getReducedCustomFieldFilters(newFilters ?? {});
  const excludeCustomFields = getReducedCustomFieldFilters(newFilters?.exclude ?? {});

  const fieldSizeFilters = Object.keys(filters?.field_size || {}).reduce((acc: any, key: string) => {
    const value = get(filters?.field_size, [key], "");
    if (key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      const transformedRecord = transformedFields?.find(f => f.field_key === key);
      let fieldKey = key;
      if (transformedRecord && transformedRecord?.metadata?.transformed) {
        fieldKey = key.replace(transformedRecord?.metadata?.transformed, "");
      }
      return { ...acc, [fieldKey]: value };
    }
    return { ...acc, [key]: value };
  }, {});

  newFilters = {
    ...newFilters,
    field_size: fieldSizeFilters,
    workitem_custom_fields: customFields,
    exclude: { ...(newFilters.exclude || {}), workitem_custom_fields: excludeCustomFields }
  };
  forEach(Object.keys(newFilters || {}), key => {
    if (key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      unset(newFilters, [key]);
    }
  });
  forEach(Object.keys(newFilters.exclude || {}), key => {
    if (key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      unset(newFilters, ["exclude", key]);
    }
  });
  return sanitizeObjectCompletely(newFilters);
};

export const spreadCustomFieldsFilter = (filters: any) => {
  let newFilters = filters;
  newFilters = {
    ...newFilters,
    ...(newFilters?.custom_fields || {}),
    exclude: {
      ...(newFilters.exclude || {}),
      ...(newFilters?.exclude?.custom_fields || {})
    }
  };
  unset(newFilters, ["exclude", "custom_fields"]);
  unset(newFilters, ["custom_fields"]);
  return newFilters;
};

export const azureSpreadCustomFieldsFilter = (filters: any, transformedFields: IntegrationTransformedCFTypes[]) => {
  let newFilters = cloneDeep(filters);
  const customFields = get(newFilters, ["workitem_custom_fields"], {});
  const excludeCustomFields = get(newFilters, ["exclude", "workitem_custom_fields"], {});
  const newCustomFields = Object.keys(customFields).reduce((acc, key) => {
    if (!key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      return { ...acc, [`${AZURE_CUSTOM_FIELD_PREFIX}${key}`]: get(customFields, [key]) };
    }
    return { ...acc, [key]: get(customFields, [key]) };
  }, {});
  const newExcludeCustomFields = Object.keys(excludeCustomFields).reduce((acc, key) => {
    if (!key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      return { ...acc, [`${AZURE_CUSTOM_FIELD_PREFIX}${key}`]: get(excludeCustomFields, [key]) };
    }
    return { ...acc, [key]: get(excludeCustomFields, [key]) };
  }, {});
  newFilters = {
    ...newFilters,
    ...(newCustomFields || {}),
    field_size: transformAzureMissingAndFieldSizeFilters(get(newFilters, ["field_size"], {}), transformedFields),
    exclude: {
      ...(newFilters.exclude || {}),
      ...(newExcludeCustomFields || {})
    }
  };
  unset(newFilters, ["exclude", "workitem_custom_fields"]);
  unset(newFilters, ["workitem_custom_fields"]);
  return newFilters;
};
