import { ReactNode } from "react";
import { SelectDropdownKeys } from "./select-dropdown.types";
import WorkspaceSelectDropdownComponent from "./workspace-select-dropdown/WorkspaceSelectDropdownComponent";

export const SELECT_DROPDOWN_MAPPING: Record<SelectDropdownKeys, ReactNode> = {
  [SelectDropdownKeys.WORKSPACE_SELECT_DROPDOWN]: WorkspaceSelectDropdownComponent
};
