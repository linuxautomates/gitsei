import { TABLE_REPORT_GET_DATA } from "reduxConfigs/actions/actionTypes";
import { BaseActionType } from "reduxConfigs/actions/restapi/action.type";

/**
 * It takes an object as a parameter, and returns an object with the same properties, but with an
 * additional property called `type`
 * @param {T} payload - T - this is the payload that will be passed to the reducer.
 * @returns An object with a type property and any other properties that are passed in.
 */
export function getTableReportData<T extends Object>(payload: T): BaseActionType & T {
  if (!Object.keys(payload).length) payload = {} as any;
  return { type: TABLE_REPORT_GET_DATA, ...payload };
}
