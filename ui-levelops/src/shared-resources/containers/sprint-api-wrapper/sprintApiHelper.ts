import { forEach, get } from "lodash";

export const trimPreviewFromId = (widgetId: string) => {
  if (!widgetId.includes("preview")) return widgetId;
  return widgetId.substring(0, widgetId.indexOf("preview") - 1);
};

// keys in metadata for which we reload
export const reloadOnMetaDataChangeKeys = [];

// keys in filters for which we don't reload
export const filterExcludeKeysForNoReload = ["metric"];

// this method returns object the will be used for reloading the api-wrapper
export const getApiCallEnableDisableObject = (
  changedDataObject: any,
  keysToEnableDisable: string[],
  excludeKeys = false
) => {
  if (excludeKeys) {
    let newchangedDataObject: any = {};
    forEach(Object.keys(changedDataObject), key => {
      if (!keysToEnableDisable.includes(key)) {
        newchangedDataObject[key] = changedDataObject[key];
      }
    });
    if (changedDataObject?.filter) {
      let newFilter: any = {};
      forEach(Object.keys(changedDataObject?.filter), key => {
        if (!keysToEnableDisable.includes(key)) {
          newFilter[key] = get(changedDataObject?.filter, [key], undefined);
        }
      });
      newchangedDataObject = {
        ...newchangedDataObject,
        filter: newFilter
      };
    }
    return newchangedDataObject;
  } else {
    let newchangedDataObject: any = {};
    forEach(Object.keys(changedDataObject), key => {
      if (keysToEnableDisable.includes(key)) {
        newchangedDataObject[key] = changedDataObject[key];
      }
    });
    return newchangedDataObject;
  }
};
