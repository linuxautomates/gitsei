import { convertEpochToDate, DateFormats } from "utils/dateUtils";

export const getReportScore = (_data: any) => {
  let response: { [key: string]: string } = {
    name: convertEpochToDate(_data?.key, DateFormats.MONTH)
  };
  (_data.report || []).forEach((item: { [key: string]: any }) => {
    if (item?.report?.org_id) {
      if ("org_name" in item?.report && !!item?.report?.org_name) {
        response = {
          ...response,
          [`${item?.report?.org_name.split(" ").join("_")}`]: item?.report?.score
        };
      }
    } else if (item?.report?.org_user_id) {
      response = {
        ...response,
        user_report: item?.report?.score
      };
    } else {
      response = {
        ...response,
        industry: item?.report?.score
      };
    }
  });
  return response;
};
