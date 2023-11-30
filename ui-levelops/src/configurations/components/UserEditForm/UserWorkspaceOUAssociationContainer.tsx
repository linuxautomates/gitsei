import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { cloneDeep, get, isEmpty, set, unset } from "lodash";
import React, { useEffect, useMemo } from "react";
import { AntButton } from "../../../shared-resources/components";
import UserWorkspaceAssociationFieldComponent from "./UserWorkspaceAssociationFieldComponent";
import { v1 as uuid } from "uuid";
import "./userWorkspaceOUAssociationContainer.styles.scss";
import { UserWorkspaceSelectionType } from "reduxConfigs/types/response/me.response";

interface UserWorkspaceOUAssociationProps {
  handleMetadataChange: (payload: any) => void;
  metadata: any;
}

const UserWorkspaceOUAssociationContainer: React.FC<UserWorkspaceOUAssociationProps> = ({
  handleMetadataChange,
  metadata
}) => {
  const { loading: workSpaceloading, workSpaceListData } = useWorkSpaceList();
  const workspaces: UserWorkspaceSelectionType = useMemo(
    () => cloneDeep(get(metadata, ["workspaces"], {})),
    [metadata]
  );

  const addWorkspace = () => {
    let nPayload = cloneDeep(metadata);
    const selections = Object.values(workspaces);
    const hasEmptyWorkspaceSelection = selections.length ? !!selections.find(v => !v.workspaceId) : false;

    if (!hasEmptyWorkspaceSelection) {
      nPayload = {
        ...(nPayload ?? {}),
        workspaces: {
          ...workspaces,
          [uuid()]: {
            workspaceId: "",
            orgUnitIds: []
          }
        }
      };
    }

    handleMetadataChange(nPayload);
  };

  useEffect(() => {
    if (isEmpty(workspaces)) {
      addWorkspace();
    }
  }, []);

  const workspaceFieldIds = useMemo(() => Object.keys(workspaces), [workspaces]);

  const handleUpdateWorkspaceSelection = (workspaces: UserWorkspaceSelectionType) => {
    let nPayload = cloneDeep(metadata);
    set(nPayload, ["workspaces"], workspaces);
    handleMetadataChange(nPayload);
  };

  const handleDeleteWorkspaceSelection = (fieldId: string) => {
    const nWorkspaces = cloneDeep(workspaces);
    unset(nWorkspaces, [fieldId]);
    handleUpdateWorkspaceSelection(nWorkspaces);
  };

  const handleWorkspaceOUSelection = (
    fieldId: string,
    field: "workspaceId" | "orgUnitIds",
    value: string | string[]
  ) => {
    let nPayload = cloneDeep(workspaces);
    if (field === "workspaceId") {
      nPayload[fieldId] = {
        workspaceId: value as string,
        orgUnitIds: []
      };
    } else {
      nPayload[fieldId] = {
        ...nPayload[fieldId],
        orgUnitIds: value as string[]
      };
    }
    handleUpdateWorkspaceSelection(nPayload);
  };

  const selectedWorkspaceIds = useMemo(
    () => Object.values(workspaces).map(workspace => workspace.workspaceId),
    [workspaces]
  );

  return (
    <div className="user-workspace-association-container">
      {workspaceFieldIds.map(id => (
        <UserWorkspaceAssociationFieldComponent
          fieldId={id}
          hasOnlyOneSelection={workspaceFieldIds.length === 1}
          selectedOrgUnits={workspaces[id]?.orgUnitIds ?? []}
          workspaceId={workspaces[id]?.workspaceId}
          selectedWorkspaceIds={selectedWorkspaceIds}
          workspaceList={workSpaceListData}
          deleteWorkspaceSelection={handleDeleteWorkspaceSelection}
          handleWorkspaceOUSelection={handleWorkspaceOUSelection}
        />
      ))}
      <AntButton icon="plus-circle" type="link" className="association-button-padding" onClick={addWorkspace}>
        select project and collections
      </AntButton>
    </div>
  );
};

export default UserWorkspaceOUAssociationContainer;
