import { useConfigScreenPermissions } from "./useConfigScreenPermissions";

export const useHasConfigReadOnlyPermission = () => {
  const accesses = useConfigScreenPermissions();
  return !accesses.reduce((previous: boolean, current: boolean) => previous || current, false);
};
