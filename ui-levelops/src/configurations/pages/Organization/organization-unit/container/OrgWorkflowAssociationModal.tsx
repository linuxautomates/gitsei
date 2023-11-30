import React, { useEffect, useMemo, useState } from "react";
import { Modal } from "antd";
import { AntRadioGroup } from "shared-resources/components";
import { useDispatch } from "react-redux";
import { workflowProfileOuAssociationAction } from "reduxConfigs/actions/restapi/workFlowNewAction";
import { VelocityConfigsListPage } from "configurations/pages/lead-time-profiles/containers";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useHistory } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import { useWorkspace } from "custom-hooks/useWorkspace";
import { useAppStore } from "contexts/AppStoreContext";
import { getIsStandaloneApp } from "helper/helper";

interface OrgWorkflowAssociationModalProps {
  showModal: boolean;
  onCancel: () => void;
  onAssociationSuccess: () => void;
  org?: orgUnitJSONType;
  workspaceId: string | string[] | null;
}

const OrgWorkflowAssociationModal: React.FC<OrgWorkflowAssociationModalProps> = ({
  showModal,
  onCancel,
  onAssociationSuccess,
  org,
  workspaceId
}) => {
  const [selectedProfile, setSelectedProfile] = useState();
  const [profilesAvailable, setProfilesAvailable] = useState(true);
  const dispatch = useDispatch();
  const history = useHistory();
  const { loading: workspaceLoading, workSpaceListData } = useWorkSpaceList();

  const onAssociateClick = () => {
    if (profilesAvailable) {
      selectedProfile && dispatch(workflowProfileOuAssociationAction(selectedProfile, org?.id || "", org?.name || ""));
      onAssociationSuccess();
    } else {
      history.push(
        WebRoutes.velocity_profile.scheme.edit(
          `new&profileType=new&defaultOU=${org?.id || ""}&ouIntegrations=${ouIntegrationIds.join(",")}`
        )
      );
    }
  };

  useEffect(() => {
    setSelectedProfile(undefined);
  }, [org]);

  const { selectedProject, accountInfo } = useAppStore();
  const { identifier: accountId = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
  const { selectedWorkspace } = useWorkspace({
    accountId,
    orgIdentifier,
    projectIdentifier
  });

  const ouIntegrationIds = useMemo(() => {
    let ouIntegrations = org?.sections
      ?.map((orgSection: any) => Object.keys(orgSection.integrations)) // fetch integrations objects as array
      .reduce((acc: string[], ar: Array<string>) => [...acc, ...ar], []) // combine itegraions in one array
      .map((item: string) => +item); // convert to int array
    if (ouIntegrations && ouIntegrations.length > 0) {
      return ouIntegrations;
    }
    if (!workspaceLoading) {
      const currentWorkspace = getIsStandaloneApp()
        ? workSpaceListData.find((workspace: WorkspaceModel) => workspace.id === workspaceId)
        : selectedWorkspace;
      if (currentWorkspace) {
        return currentWorkspace.integration_ids;
      }
    }
    return [];
  }, [org, workspaceId, workSpaceListData]);

  return (
    <Modal
      visible={showModal}
      onOk={onAssociateClick}
      onCancel={onCancel}
      okText={profilesAvailable ? "Associate Profile" : "Create new profile"}
      cancelText="Cancel"
      title="Select workflow profile for association"
      okButtonProps={{
        disabled: !selectedProfile && profilesAvailable
      }}>
      <AntRadioGroup value={selectedProfile} onChange={(e: any) => setSelectedProfile(e.target.value)}>
        <VelocityConfigsListPage
          isModalView
          ouIntegrationIds={ouIntegrationIds}
          setProfilesAvailable={setProfilesAvailable}
        />
      </AntRadioGroup>
    </Modal>
  );
};

export default OrgWorkflowAssociationModal;
