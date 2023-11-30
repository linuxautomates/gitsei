import { AnalyticsCategoryType, LoginAnalyticsActions } from "dataTracking/analytics.constants";
import { emitEvent } from "dataTracking/google-analytics";
import { MeResponse } from "reduxConfigs/types/response/me.response";

/**
 * Anything that we need to do after successfully logging the user in
 */
export const postSuccessfulLogin = (userData: MeResponse) => {
  // GA event login_flow
  emitEvent(AnalyticsCategoryType.LOGIN_FLOW, LoginAnalyticsActions.LOGIN_SUCCESS, userData.email);
};
