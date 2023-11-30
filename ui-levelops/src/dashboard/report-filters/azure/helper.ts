import { get } from "lodash";

export const extractFilterAPIData = (args: any, key: string) => {
  const filterMetaData = get(args, ["filterMetaData"], {});
  const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
  const currData = filterApiData.find((item: any) => Object.keys(item)[0] === key);
  return get(currData, [key, "records"], []);
};
