import { useDashboardPermissions } from "./useDashboardPermissions";

export const useIsDashboardReadonly = () => {
  const accesses = useDashboardPermissions();
  return !accesses.reduce((previous: boolean, current: boolean) => previous || current, false);
};
