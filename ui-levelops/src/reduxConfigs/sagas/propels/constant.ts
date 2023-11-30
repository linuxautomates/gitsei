import { WORKSPACES } from "dashboard/constants/applications/names";
import { ProjectToWorkspaceMappingKeys } from "../saga-types/propels.types";

export const PROJECT_WORKSPACE_MAPPING: Record<ProjectToWorkspaceMappingKeys, string> = {
  [ProjectToWorkspaceMappingKeys.NAME]: WORKSPACES,
  [ProjectToWorkspaceMappingKeys.DESCRIPTION]: "The Propels workspaces associated with the ticket.",
  [ProjectToWorkspaceMappingKeys.URI]: "workspace"
};
