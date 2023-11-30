import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { DEV_PROD_PR_ACTIVITY_ID } from "dashboard/pages/scorecard/constants";
import {
  OU_SCORE_OVERVIEW,
  RESTAPI_READ,
  DEV_PRODUCTIVITY_SCORE_REPORT_LIST,
  DEV_PRODUCTIVITY_PR_ACTIVITY
} from "../actionTypes";

export const trellisReport = (id: string, filters: any) => ({
  type: RESTAPI_READ,
  uri: "dev_productivity_reports",
  method: "list",
  id: id,
  data: filters
});

export const trellisUserDetails = (id: string, params: any) => ({
  type: RESTAPI_READ,
  uri: "dev_productivity_reports",
  method: "get",
  id: id,
  queryparams: params
});
export const trellisRelativeScore = (id: string, filters: any) => ({
  type: RESTAPI_READ,
  uri: "dev_productivity_relative_score",
  method: "list",
  id: id,
  data: filters
});

export const trellisScoreReport = (id: string, filters: any, complete = null) => ({
  type: DEV_PRODUCTIVITY_SCORE_REPORT_LIST,
  uri: "dev_productivity_score_report",
  method: "list",
  id: id,
  data: filters,
  complete
});

export const ouScoreOverview = (id: string, data: basicMappingType<any>) => ({
  type: OU_SCORE_OVERVIEW,
  id,
  data
});

export const trellisPRActivityAction = (
  user_id_type: any,
  user_id: any,
  time_range: any,
  trellis_profile_id: string,
  profileKey: Record<string, any> | undefined
) => ({
  type: DEV_PRODUCTIVITY_PR_ACTIVITY,
  id: DEV_PROD_PR_ACTIVITY_ID,
  user_id_type,
  user_id,
  time_range,
  trellis_profile_id,
  profileKey
});
