import { CustomTimeBasedTypes } from "../helper";

interface customFieldType {
  key: string;
  metadata: {
    transformed: string;
  };
  name: string;
}
interface FieldListType {
  key: string;
  name: string;
  type: string;
}
/**
 *  Removes the custom date range filters for add filters in widget settings
 *
 */
export const removeCustomDateFields = (customFields: customFieldType[], fieldList: FieldListType[]) => {
  return (customFields || [])?.filter((item: any) => {
    const fieldItem = fieldList.find((field: any) => field?.key === item?.key);
    if (fieldItem && CustomTimeBasedTypes.includes(fieldItem?.type)) {
      return false;
    }
    return true;
  });
};
