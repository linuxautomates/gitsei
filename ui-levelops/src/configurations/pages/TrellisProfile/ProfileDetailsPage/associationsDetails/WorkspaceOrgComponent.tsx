import { Modal, Popconfirm, Select, Spin } from "antd";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { getWorkspaceOUList } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOUsForWorkspace } from "reduxConfigs/selectors/trellisProfileSelectors";
import { AntButton, AntSelect, AntText } from "shared-resources/components";

interface WorkspaceOrgComponentProps {
  workspaceList: WorkspaceModel[];
  selectedWorkspacesList: string[];
  workspaceId: string;
  setSelectedWorkspaces: (previousWorkspaceId: string, workspaceId: string) => void;
  handleOrgChanges: (workspaceId: string, value: Array<string>) => void;
  deleteWorkspaceSection: (index: string) => void;
  profile: RestTrellisScoreProfile;
  profilesList: Array<any>;
  selectedOrgs?: Array<string>;
}
const { confirm } = Modal;

const WorkspaceOrgComponent = (props: WorkspaceOrgComponentProps) => {
  const {
    workspaceList,
    selectedWorkspacesList,
    workspaceId,
    setSelectedWorkspaces,
    handleOrgChanges,
    profile,
    profilesList,
    selectedOrgs,
    deleteWorkspaceSection
  } = props;

  const [orgList, setOrgList] = useState<Array<any>>([]);
  const dispatch = useDispatch();
  const OUsForWorkspaceState = useParamSelector(getOUsForWorkspace, { workspaceId });
  const selectedOrgCount = useMemo(() => (selectedOrgs ? selectedOrgs.length : 0), [selectedOrgs]);

  const isHarnessReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.TRELLIS_PROFILE_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;

  const showConfirm = (newWorkspaceId: string) => {
    confirm({
      title: "Selecting a different project will reset the current Collection selections. Are you sure?",
      onOk: () => setSelectedWorkspaces(workspaceId, newWorkspaceId)
    });
  };

  const handleWorkspaceChange = (newWorkspaceId: string, forceUpdate: boolean = false) => {
    if (selectedOrgCount && !forceUpdate) {
      showConfirm(newWorkspaceId);
      return;
    }
    setSelectedWorkspaces(workspaceId, newWorkspaceId);
  };

  useEffect(() => {
    if (workspaceId) {
      dispatch(getWorkspaceOUList(workspaceId));
    }
  }, [workspaceId]);

  useEffect(() => {
    if (OUsForWorkspaceState && !OUsForWorkspaceState.isLoading) {
      setOrgList(OUsForWorkspaceState.OUlist || []);
    }
  }, [OUsForWorkspaceState]);

  const associatedOrgs = useMemo(() => {
    return profilesList.reduce((orgsList: Array<string>, orgProfile: any) => {
      if (profile.id !== orgProfile.id && orgProfile.associated_ou_ref_ids) {
        orgsList = orgsList.concat(orgProfile.associated_ou_ref_ids);
      }
      return orgsList;
    }, []);
  }, [profilesList]);

  const getWorkspaceId = useMemo(() => {
    const workspace = workspaceList.find(workspace => {
      return workspace.id === workspaceId;
    });
    if (!workspace) {
      handleWorkspaceChange("", true);
    }
    return workspace?.id;
  }, [workspaceList, workspaceId]);

  const workspaceDropdown = useMemo(() => {
    const disabledWorkspaceList = selectedWorkspacesList.filter((_workspaceId: string) => _workspaceId != workspaceId);
    return (
      <div className="dev-score-profile-container-section-container-header w-40 mr-80">
        <AntText className="section-header">PROJECT</AntText>
        <AntText className="section-sub-header">Select a project to view available Collections.</AntText>
        <AntSelect
          showSearch
          className="selector"
          key="workspace"
          value={getWorkspaceId}
          placeholder={"Select Project"}
          onChange={handleWorkspaceChange}
          filterOption={(input: any, option: any) => option.props.label.toLowerCase().includes(input.toLowerCase())}>
          {workspaceList.map((workspace: WorkspaceModel) => (
            <Select.Option
              value={workspace.id}
              key={workspace.id}
              label={workspace.name}
              disabled={disabledWorkspaceList.includes(workspace.id) || workspace?.demo === true}>
              {workspace.name}
            </Select.Option>
          ))}
        </AntSelect>
      </div>
    );
  }, [workspaceId, workspaceList, selectedWorkspacesList]);

  const orgDropdown = useMemo(() => {
    return (
      <div className="dev-score-profile-container-section-container-header w-40 mr-80">
        <AntText className="section-header">COLLECTION</AntText>
        <AntText className="section-sub-header">Select the Collections that may apply this profile.</AntText>
        <AntSelect
          className="selector"
          key="OrgUnit"
          mode="multiple"
          value={selectedOrgs}
          placeholder={"Select Collections"}
          disabled={!workspaceId}
          onChange={(values: string[]) => handleOrgChanges(workspaceId, values)}
          filterOption={(input: any, option: any) => option.props.label.toLowerCase().includes(input.toLowerCase())}>
          {orgList.map((org: any) => (
            <Select.Option value={org.id} key={org.id} label={org.name} disabled={associatedOrgs.includes(org.id)}>
              {org.name}
            </Select.Option>
          ))}
        </AntSelect>
      </div>
    );
  }, [workspaceId, selectedOrgs, associatedOrgs, handleOrgChanges, orgList]);

  return (
    <div className="flex px-40 py-20">
      {workspaceDropdown}
      {OUsForWorkspaceState?.isloading && workspaceId ? (
        <div className="rest-table-spinner">
          <Spin />
        </div>
      ) : (
        <>
          {orgDropdown}
          <Popconfirm
            key={"delete-action"}
            title={"Do you want to delete this item?"}
            onConfirm={e => deleteWorkspaceSection(workspaceId)}
            okText={"Yes"}
            cancelText={"No"}>
            <AntButton icon="delete" className="margin-center" disabled={isReadOnly} />
          </Popconfirm>
        </>
      )}
    </div>
  );
};

export default WorkspaceOrgComponent;
