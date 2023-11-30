import { Icon, Modal, Popconfirm, Select } from "antd";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import Loader from "components/Loader/Loader";
import { getSelectedIntegrationIds } from "configurations/pages/lead-time-profiles/helpers/profileIntHelper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { getWorkspaceOUList } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOUsForWorkspace } from "reduxConfigs/selectors/trellisProfileSelectors";
import { AntButton, AntSelect, AntText } from "shared-resources/components";
import { OU_SELECTION_DISABLED_MESSAGE, WORKSPACE_SELECTION_DISABLED_MESSAGE } from "../constant";
import WorkflowAssociationOuImpectedListModalComponent from "./associations-ou-modal/WorkflowAssociationOuImpectedListModal";
import cx from "classnames";
import { arrayOfObjFilterAndMap } from "utils/arrayUtils";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";
interface WorkflowWorkspaceOrgComponentProps {
  workspaceList: WorkspaceModel[];
  selectedWorkspacesList: string[];
  workspaceId: string;
  setSelectedWorkspaces: (previousWorkspaceId: string, workspaceId: string) => void;
  handleOrgChanges: (workspaceId: string, value: Array<string>) => void;
  deleteWorkspaceSection: (index: string) => void;
  profile: RestWorkflowProfile;
  profilesList: Array<any>;
  selectedOrgs?: Array<string>;
  setOrgWarningPopupFlag: (type: boolean) => void;
}
const { confirm } = Modal;

const WorkflowWorkspaceOrgComponent = (props: WorkflowWorkspaceOrgComponentProps) => {
  const {
    workspaceList,
    selectedWorkspacesList,
    workspaceId,
    setSelectedWorkspaces,
    handleOrgChanges,
    profile,
    profilesList,
    selectedOrgs,
    deleteWorkspaceSection,
    setOrgWarningPopupFlag
  } = props;

  const [orgList, setOrgList] = useState<Array<any>>([]);
  const [openImpectedOuList, setOpenImpectedOuList] = useState<boolean>(false);
  const [impectedOuList, setImpectedOuList] = useState<Array<string>>([]);

  const dispatch = useDispatch();
  const OUsForWorkspaceState = useParamSelector(getOUsForWorkspace, { workspaceId });
  const selectedOrgCount = useMemo(() => (selectedOrgs ? selectedOrgs.length : 0), [selectedOrgs]);
  const LTFCAndMTTRSupport = useHasEntitlements(Entitlement.LTFC_MTTR_DORA_IMPROVEMENTS, EntitlementCheckType.AND);

  const [disabledWorkspaceListData, setDisabledWorkspaceListData] = useState<Array<any>>([]);

  const isHarnessReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;

  const selectedIntegrationIds = useMemo(
    () => getSelectedIntegrationIds(profile, LTFCAndMTTRSupport),
    [profile, LTFCAndMTTRSupport]
  );

  const showConfirm = (newWorkspaceId: string) => {
    confirm({
      title: "Selecting a different project will reset the current collection selections. Are you sure?",
      onOk: () => setSelectedWorkspaces(workspaceId, newWorkspaceId)
    });
  };

  const handleWorkspaceChange = (newWorkspaceId: string) => {
    if (selectedOrgCount) {
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
    if (OUsForWorkspaceState && !OUsForWorkspaceState.isloading) {
      setOrgList(OUsForWorkspaceState.OUlist || []);
    }
  }, [OUsForWorkspaceState]);

  const hasSelectedIntegrations = (integrationIds: Array<number>) => {
    let returnflag = false;
    if (integrationIds && integrationIds.length > 0) {
      returnflag = selectedIntegrationIds.every(int => integrationIds.includes(int));
    }
    return returnflag;
  };

  const checkIntegrationInOrg = (orgList: Array<any>, orgListData: Array<any>) => {
    return orgList.reduce((acc: Array<string>, org: any) => {
      if (!orgListData.includes(org.id)) {
        if (org.sections.length > 0) {
          // if org has integration added manually.
          let ouIntegrations = org.sections
            .map((orgSection: { integrations: any }) => {
              // Not all orgSection will have the key integrations
              if (orgSection.integrations) {
                return Object.keys(orgSection.integrations);
              } else {
                return [];
              }
            }) // fetch integrations objects as array
            .reduce((acc: string[], ar: Array<string>) => [...acc, ...ar], []) // combine itegraions in one array
            .map((item: string) => +item); // convert to int array

          if (!hasSelectedIntegrations(ouIntegrations)) {
            acc.push(org.id);
          }
        }
      }
      return acc;
    }, []);
  };

  const associatedOrgs = useMemo(() => {
    // if org is associated in other profiles
    let orgListData = profilesList.reduce((orgsList: Array<string>, orgProfile: any) => {
      if (profile.id !== orgProfile.id && orgProfile.associated_ou_ref_ids) {
        orgsList = orgsList.concat(orgProfile.associated_ou_ref_ids);
      }
      return orgsList;
    }, []);

    // check if workspace have selected integration or not
    let intIdsToRemove: string[] = [];
    let workspaceData = workspaceList.find(workspace => workspace.id === workspaceId);
    if (workspaceData && workspaceData.integration_ids.length > 0) {
      if (!hasSelectedIntegrations(workspaceData.integration_ids)) {
        intIdsToRemove = [...orgList.map(orgData => orgData.id)];
      } else {
        // check if the selected integration is valid or not in orgs of workspace
        intIdsToRemove = checkIntegrationInOrg(orgList, orgListData);
      }
    }

    // get name of impected ou list that we need to show in popup
    // update in wrokflow profile json need to remove ids from associated_ou_ref_ids key
    const impectedOuList = intIdsToRemove.filter(element => selectedOrgs?.includes(element));
    if (impectedOuList.length > 0) {
      const updateOuSelection: any = selectedOrgs?.filter(element => !impectedOuList.includes(element));
      handleOrgChanges(workspaceId, updateOuSelection);

      let impectedOuListName = orgList.reduce((acc: Array<string>, data: any) => {
        if (impectedOuList.includes(data.id)) {
          acc.push(data.name);
        }
        return acc;
      }, []);
      setImpectedOuList(impectedOuListName);
    }

    //all id that will be disabled due to selection in other profile or integration is not matching
    orgListData = [...orgListData, ...intIdsToRemove];

    return orgListData;
  }, [profilesList, orgList, selectedOrgs, workspaceId]);

  const checkWorkspaceDisabled = (disabledWorkspaceList: any, workspace: any) => {
    if (
      disabledWorkspaceList.includes(workspace.id) ||
      workspace?.demo === true ||
      !hasSelectedIntegrations(workspace.integration_ids)
    ) {
      return true;
    }
  };

  const handleOpenImpectedOuList = () => {
    setOpenImpectedOuList(true);
    setOrgWarningPopupFlag(false);
  };

  const workspaceDropdownSelection = useMemo(() => {
    return workspaceList.map((workspace: WorkspaceModel) => {
      let disabledFlag = checkWorkspaceDisabled(disabledWorkspaceListData, workspace);
      return (
        <Select.Option
          value={workspace.id}
          key={workspace.id}
          label={workspace.name}
          title={disabledFlag ? WORKSPACE_SELECTION_DISABLED_MESSAGE : ""}
          disabled={disabledFlag}>
          {workspace.name}
        </Select.Option>
      );
    });
  }, [workspaceId, workspaceList, selectedWorkspacesList, disabledWorkspaceListData]);

  const workspaceDropdown = useMemo(() => {
    const disabledWorkspaceList = selectedWorkspacesList.filter((_workspaceId: string) => _workspaceId !== workspaceId);
    setDisabledWorkspaceListData(disabledWorkspaceList);
    return (
      <div className="dev-score-profile-container-section-container-header w-40 mr-80">
        <AntText className="section-header">PROJECT</AntText>
        <AntText className="section-sub-header">Select a project to view available Collection.</AntText>
        <AntSelect
          showSearch
          className="selector"
          key="workspace"
          value={workspaceId}
          placeholder={"Select Project"}
          onChange={handleWorkspaceChange}
          filterOption={(input: any, option: any) => option.props.label.toLowerCase().includes(input.toLowerCase())}>
          {workspaceDropdownSelection}
        </AntSelect>
      </div>
    );
  }, [workspaceId, workspaceList, selectedWorkspacesList]);

  const orgDropdownSelection = useMemo(() => {
    return orgList.map((org: any) => {
      let disabledFlag = associatedOrgs.includes(org.id);
      return (
        <Select.Option
          value={org.id}
          key={org.id}
          label={org.name}
          disabled={disabledFlag}
          title={disabledFlag ? OU_SELECTION_DISABLED_MESSAGE : ""}>
          {org.name}
        </Select.Option>
      );
    });
  }, [workspaceId, orgList, selectedOrgs, associatedOrgs]);

  const selectedActiveOrgs = useMemo(() => {
    if (orgList?.length > 0) {
      return arrayOfObjFilterAndMap(orgList, selectedOrgs);
    }
  }, [orgList, selectedOrgs]);

  const orgDropdown = useMemo(() => {
    return (
      <div className="dev-score-profile-container-section-container-header org-unit-mapping-container w-40 mr-80">
        <AntText className="section-header">COLLECTION</AntText>
        <AntText className="section-sub-header">Select the Collections that may apply this profile.</AntText>
        <Select
          className={cx("selector", { "selector-red-bottom": impectedOuList && impectedOuList?.length > 0 })}
          key="OrgUnit"
          mode="multiple"
          value={selectedActiveOrgs}
          placeholder={"Select Collections"}
          disabled={!workspaceId}
          onChange={(values: string[]) => handleOrgChanges(workspaceId, values)}
          notFoundContent={OUsForWorkspaceState.isloading ? <Loader /> : null}
          filterOption={(input: any, option: any) => option.props.label.toLowerCase().includes(input.toLowerCase())}>
          {orgDropdownSelection}
        </Select>
        {impectedOuList && impectedOuList.length > 0 && (
          <div className="flex">
            <div></div>
            <div className="association-warning">
              <div className="association-warning-text-div">
                <Icon type="close-circle" className="icon-style" theme="filled" twoToneColor="rgb(223, 165, 42)" />
                <span className="association-warning-text">
                  Some of the collections are not related to the integration selected.
                </span>
              </div>
              <div className="association-warning-ou-div">
                <span className="association-warning-ou" onClick={handleOpenImpectedOuList}>
                  Click here
                </span>{" "}
                to view the complete list.
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }, [
    workspaceId,
    orgDropdownSelection,
    selectedOrgs,
    associatedOrgs,
    handleOrgChanges,
    OUsForWorkspaceState.isloading,
    impectedOuList,
    handleOpenImpectedOuList
  ]);

  const handleCloseImpectedOuList = () => {
    setOpenImpectedOuList(false);
    setOrgWarningPopupFlag(false);
  };

  useEffect(() => {
    if (impectedOuList && impectedOuList.length > 0) {
      setOrgWarningPopupFlag(true);
    }
  }, [impectedOuList]);

  return (
    <>
      <div className="flex py-20">
        {workspaceDropdown}
        {orgDropdown}
        <Popconfirm
          key={"delete-action"}
          title={"Do you want to delete this item?"}
          onConfirm={e => deleteWorkspaceSection(workspaceId)}
          okText={"Yes"}
          cancelText={"No"}>
          <AntButton icon="delete" className="margin-center" disabled={isReadOnly} />
        </Popconfirm>
      </div>

      <WorkflowAssociationOuImpectedListModalComponent
        handleCancel={handleCloseImpectedOuList}
        showWarningModal={openImpectedOuList}
        impectedOuList={impectedOuList}
      />
    </>
  );
};

export default WorkflowWorkspaceOrgComponent;
