import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";

export const removeCustomPrefix = (record: Record<string, any> = {}) => {
  if (record?.hasOwnProperty("metadata") && record?.metadata?.transformed) {
    return record?.key?.replace(AZURE_CUSTOM_FIELD_PREFIX, "");
  }
  return record.key;
};
