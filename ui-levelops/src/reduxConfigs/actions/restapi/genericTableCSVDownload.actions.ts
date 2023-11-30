import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { genericTableCSVDownloadActionType } from "reduxConfigs/sagas/csv-download-saga/genericTableCSVDownload.saga";
import { GENERIC_TABLE_CSV_DOWNLOAD } from "../actionTypes";

export const genericTableCSVDownload: (
  uri: string,
  method: string,
  data: {
    transformer: ((data: any) => any) | undefined;
    filters: { page_size: number; page: number; [x: string]: any };
    columns: any[];
    derive?: boolean;
    shouldDerive?: any;
    jsxHeaders?: { title: string; key: string }[];
  },
  queryparams?: basicMappingType<any>
) => genericTableCSVDownloadActionType = (uri, method, apidata, queryparams = {}) => {
  return {
    type: GENERIC_TABLE_CSV_DOWNLOAD,
    uri,
    method,
    data: apidata,
    queryparams
  };
};
