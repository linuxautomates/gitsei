import { createSelector } from "reselect";

export const sessionState = (state: any) => state.sessionReducer;

export const sessionMFASelector = createSelector(sessionState, state => state?.session_mfa);
export const sessionMFAEnrollSelector = createSelector(sessionState, state => state?.session_mfa_enroll);
