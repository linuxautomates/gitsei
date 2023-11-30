import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntButton, CustomSpinner } from "shared-resources/components";
import WorkflowAssociationOuImpectedModalComponent from "./associations-ou-modal/WorkflowAssociationOuImpectedModal";
import WorkflowWorkspaceOrgComponent from "./WorkflowWorkspaceOrgComponent";

interface WorkflowAssociationsOrgSelectorProps {
  profile: RestWorkflowProfile;
  handleChanges: (value: any, type: string) => void;
  profilesList: Array<any>;
  setExclamationFlag: (value: boolean) => void;
}

const WorkflowAssociationsOrgSelector: React.FC<WorkflowAssociationsOrgSelectorProps> = ({
  profile,
  handleChanges,
  profilesList,
  setExclamationFlag
}) => {
  const { loading: workSpaceloading, workSpaceListData } = useWorkSpaceList();

  const [orgWarningPopupFlag, setOrgWarningPopupFlag] = useState<boolean>(false);

  const stringify = (id: number | string = "") => `w_${id}`;

  const setWorkspace = useCallback(
    (previousWorkspaceId: string, workspaceId: string) => {
      const index: number = Object.keys(profile.workspace_to_org).findIndex(
        (key: string) => key === stringify(previousWorkspaceId)
      );
      let keyValues = Object.entries(profile.workspace_to_org);
      keyValues.splice(index, 1, [stringify(workspaceId), []]);
      const updatedObj = Object.fromEntries(keyValues);
      handleChanges(updatedObj, "workspace_to_org");
    },
    [profile.workspace_to_org]
  );

  const addWorkspace = useCallback(() => {
    handleChanges(
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
        {
          ...profile.workspace_to_org,
          [stringify(workspaceId)]: values
        },
        "workspace_to_org"
      );
    },
    [profile.workspace_to_org]
  );

  const workspaceIdList = useMemo(
    () => Object.keys(profile.workspace_to_org).map((key: string) => key.slice(2)),
    [profile.workspace_to_org]
  );

  const handleCloseOrgWarningPopup = () => {
    setOrgWarningPopupFlag(false);
  };

  useEffect(() => {
    if (orgWarningPopupFlag) {
      setExclamationFlag(true);
    } else {
      setExclamationFlag(false);
    }
  }, [orgWarningPopupFlag]);

  if (workSpaceloading) {
    return <CustomSpinner />;
  }
  return (
    <>
      {workspaceIdList.map((workspaceId: string) => {
        return (
          <WorkflowWorkspaceOrgComponent
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
            setOrgWarningPopupFlag={setOrgWarningPopupFlag}
          />
        );
      })}
      <AntButton icon="plus-circle" type="link" className="association-button-padding" onClick={addWorkspace}>
        Add Collection from a different project
      </AntButton>

      <WorkflowAssociationOuImpectedModalComponent
        handleCancel={handleCloseOrgWarningPopup}
        showWarningModal={orgWarningPopupFlag}
        handleProceed={handleCloseOrgWarningPopup}
      />
    </>
  );
};

export default WorkflowAssociationsOrgSelector;
