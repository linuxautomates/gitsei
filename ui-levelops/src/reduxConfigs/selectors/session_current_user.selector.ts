import { getBaseUrl } from "constants/routePaths";
import { get } from "lodash";
import { MeResponse } from "reduxConfigs/types/response/me.response";
import { createSelector } from "reselect";
import { sessionState } from "./session_mfa.selector";

export const LICENSE_TRIAL = "limited_trial";

export const sessionUserState = createSelector(sessionState, (session: any) => {
  return get(session, ["session_current_user"], {});
});

export const sessionCurrentUserState = createSelector(sessionUserState, (session: any) => {
  return get(session, ["data"], {});
});

export const sessionUserLastLoginURL = createSelector(sessionCurrentUserState, (data: any) => {
  return get(data, ["metadata", "last_login_url"], getBaseUrl());
});

export const sessionUserWorkspacesSelections = createSelector(sessionCurrentUserState, (data: any) => {
  return get(data, ["metadata", "workspaces"], {});
});
export const sessionUserMangedOURefs = createSelector(sessionCurrentUserState, (data: any) => {
  return get(data, ["managed_ou_ref_ids"], undefined);
});

export const sessionCurrentUserLicense = createSelector(sessionCurrentUserState, (user: MeResponse) => {
  return user?.license;
});
export const isSelfOnboardingUser = createSelector(sessionCurrentUserState, (user: MeResponse) => {
  /** Will use this when we recieve the licence field in the response */
  return user?.license === LICENSE_TRIAL;
});

export const selfOnboardingUsersEntitlements = createSelector(
  sessionCurrentUserState,
  (user: MeResponse) => user?.entitlements ?? []
);

/** selector for selecting number of repos allowed to select by a trial user*/
export const allowedSCMReposCountForUser = createSelector(
  isSelfOnboardingUser,
  selfOnboardingUsersEntitlements,
  (isTrialUser: boolean, entitlements: any[]) => {
    if (isTrialUser) {
      const corEntitlement = entitlements.find(en => Object.keys(en)[0] === "ALLOWED_SCM_REPOS"); // using temp entitlement
      return corEntitlement ? (Object.values(corEntitlement)[0] as number) : 10;
    }
    return -1;
  }
);
