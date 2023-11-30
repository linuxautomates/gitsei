import { cloneDeep, get, set } from "lodash";
import { UserWorkspaceSelectionType } from "reduxConfigs/types/response/me.response";
import { v1 as uuid } from "uuid";
/**
 * "If the user is a ORG_UNIT_ADMIN, then they must have at least one collection selected."
 * @param {UserWorkspaceSelectionType} workspaceSelection - UserWorkspaceSelectionType
 * @returns A boolean value.
 */
export function isInValidOrgUnitAdminUser(workspaceSelection: UserWorkspaceSelectionType) {
  const selections = Object.values(workspaceSelection);
  if (!selections.length) return true;
  if (selections.length === 1) {
    return !selections[0]?.orgUnitIds?.length;
  }
  return false;
}

/**
 * It takes a metadata object and returns a new metadata object with the workspaceIds replaced with
 * uuids
 * @param metadata - The metadata object that you want to transform.
 * @returns A function that takes a metadata object and returns a new metadata object with the
 * workspaces key transformed.
 */
export function transformUsersMetadata(metadata: { workspaces: Record<string, string[]> }) {
  const nMetadata = cloneDeep(metadata);
  const workspaces = get(metadata, ["workspaces"], {});
  const workspaceIds = Object.keys(workspaces);
  const newWorkspaces = workspaceIds.reduce((acc, next) => {
    return {
      ...acc,
      [uuid()]: {
        workspaceId: next,
        orgUnitIds: (workspaces as Record<string, string[]>)[next]
      }
    };
  }, {});
  set(nMetadata, ["workspaces"], newWorkspaces);
  return nMetadata;
}
