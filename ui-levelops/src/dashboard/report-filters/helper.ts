import { get } from "lodash";

export const extractFilterAPIData = (args: any, key: string) => {
  const filterMetaData = get(args, ["filterMetaData"], {});
  const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
  const currData = filterApiData.find((item: any) => Object.keys(item)[0] === key);
  if (currData) {
    return Object.values(currData)[0] as Array<any>;
  }
  return [];
};

export const genericGetFilterAPIData = (args: any, key: string, labelKey: string = "key", valueKey: string = "key") => {
  const data = extractFilterAPIData(args, key);
  return data
    ?.map((item: any) => ({
      label: item[labelKey],
      value: item[valueKey]
    }))
    .filter((item: { label: string; value: string }) => !!item.value);
};
