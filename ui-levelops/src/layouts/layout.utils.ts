import { NO_SCOPE } from "./layouts.constants";

export const isNoScopeActive = (searchParam: string): boolean => {
  return searchParam === NO_SCOPE;
};

export const isProjectSelectedFromNoScopeState = (currentLocation: string, isNav2Enabled?: boolean): boolean => {
  return (
    !!isNav2Enabled &&
    currentLocation.includes("projects") &&
    currentLocation.includes("orgs") &&
    currentLocation.includes("/home") &&
    !currentLocation.includes(NO_SCOPE)
  );
};

export const isAccountSelectedFromNoScopeState = (currentLocation: string, isNav2Enabled?: boolean): boolean => {
  return (
    !!isNav2Enabled &&
    currentLocation.includes("account") &&
    currentLocation.includes("/home") &&
    !currentLocation.includes(NO_SCOPE)
  );
};
