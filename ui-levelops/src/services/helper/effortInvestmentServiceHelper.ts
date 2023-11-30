import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { get } from "lodash";

/**
 * This function adds Custom. as prefix to those custom field where it is not present.
 *
 * @param apiData Custom field records
 * @returns
 */
export const getTransformedCustomFieldData = (apiData: any, prefix = AZURE_CUSTOM_FIELD_PREFIX) => {
  const records = get(apiData, ["data", "records"], []);
  const newRecords = records.map((item: any) => {
    const key = item?.field_key;
    if (key.includes(prefix)) {
      return item;
    }
    return {
      ...item,
      field_key: `${prefix}${item.field_key}`,
      metadata: {
        transformed: prefix
      }
    };
  });

  return {
    ...apiData,
    data: {
      ...apiData?.data,
      records: newRecords
    }
  };
};
