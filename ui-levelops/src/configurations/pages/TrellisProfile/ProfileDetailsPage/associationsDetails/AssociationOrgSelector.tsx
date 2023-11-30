import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import React, { useCallback, useMemo } from "react";
import { AntButton, CustomSpinner } from "shared-resources/components";
import WorkspaceOrgComponent from "./WorkspaceOrgComponent";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";

interface AssociationsOrgSelectorProps {
  profile: RestTrellisScoreProfile;
  handleChanges: (section_name: string, value: any, type: string) => void;
  profilesList: Array<any>;
}

const AssociationsOrgSelector: React.FC<AssociationsOrgSelectorProps> = ({ profile, handleChanges, profilesList }) => {
  const { loading: workSpaceloading, workSpaceListData } = useWorkSpaceList();

  const stringify = (id: number | string = "") => `w_${id}`;

  const setWorkspace = useCallback(
    (previousWorkspaceId: string, workspaceId: string) => {
      const index: number = Object.keys(profile.workspace_to_org).findIndex(
        (key: string) => key === stringify(previousWorkspaceId)
      );
      let keyValues = Object.entries(profile.workspace_to_org);
      keyValues.splice(index, 1, [stringify(workspaceId), []]);
      const updatedObj = Object.fromEntries(keyValues);
      handleChanges("", updatedObj, "workspace_to_org");
    },
    [profile.workspace_to_org]
  );

  const addWorkspace = useCallback(() => {
    handleChanges(
      "",
      {
        ...profile.workspace_to_org,
        [stringify("")]: []
      },
      "workspace_to_org"
    );
  }, [profile.workspace_to_org]);

  const removeWorkspace = useCallback(
    (workspaceId: string) => {
      delete profile.workspace_to_org[stringify(workspaceId)];
      handleChanges(
        "",
        {
          ...profile.workspace_to_org
        },
        "workspace_to_org"
      );
    },
    [profile.workspace_to_org]
  );

  const updateOrgList = useCallback(
    (workspaceId: string, values: Array<string>) => {
      handleChanges(
        "",
        {
          ...profile.workspace_to_org,
          [stringify(workspaceId)]: values
        },
        "workspace_to_org"
      );
    },
    [profile.workspace_to_org]
  );

  const isHarnessReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.TRELLIS_PROFILE_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;

  const workspaceIdList = useMemo(
    () => Object.keys(profile.workspace_to_org).map((key: string) => key.slice(2)),
    [profile.workspace_to_org]
  );

  if (workSpaceloading) {
    return <CustomSpinner />;
  }
  return (
    <>
      {workspaceIdList.map((workspaceId: string) => {
        return (
          <WorkspaceOrgComponent
            workspaceList={workSpaceListData}
            selectedWorkspacesList={workspaceIdList}
            workspaceId={workspaceId}
            setSelectedWorkspaces={setWorkspace}
            key={workspaceId}
            handleOrgChanges={updateOrgList}
            deleteWorkspaceSection={removeWorkspace}
            profile={profile}
            profilesList={profilesList}
            selectedOrgs={profile.workspace_to_org[stringify(workspaceId)]}
          />
        );
      })}
      {!isReadOnly && (
        <AntButton icon="plus-circle" type="link" className="m-20" onClick={addWorkspace}>
          Add Collections from a different workspace
        </AntButton>
      )}
    </>
  );
};

export default AssociationsOrgSelector;
