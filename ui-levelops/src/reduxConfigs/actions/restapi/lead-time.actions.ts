import { LEAD_TIME_BY_TIME_SPENT_IN_STAGES } from "../actionTypes";

export const leadTimeByTimeSpentInStagesActionType = (
  uri: string,
  method: string,
  filters: any,
  complete?: any,
  id?: string
) => ({
  type: LEAD_TIME_BY_TIME_SPENT_IN_STAGES,
  uri,
  method,
  filters,
  id
});
