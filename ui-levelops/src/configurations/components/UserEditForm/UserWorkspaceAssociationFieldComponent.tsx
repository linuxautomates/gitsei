import { Popconfirm, Select } from "antd";
import Loader from "components/Loader/Loader";
import { createDataTree } from "configurations/pages/Organization/organization-unit/components/org-unit-tree-view/helper";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { cloneDeep } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { getWorkspaceOUList } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOUsForWorkspace } from "reduxConfigs/selectors/trellisProfileSelectors";
import { arrayOfObjFilterAndMap } from "utils/arrayUtils";
import { AntButton, AntSelect, AntText } from "../../../shared-resources/components";
import OrgTree from "../../../shared-resources/components/popup-tree/OrgTree";

interface UserWorkspaceAssociationFieldProps {
  fieldId: string;
  workspaceId: string;
  selectedOrgUnits: string[];
  selectedWorkspaceIds: string[];
  workspaceList: WorkspaceModel[];
  hasOnlyOneSelection: boolean;
  deleteWorkspaceSelection: (id: string) => void;
  handleWorkspaceOUSelection: (
    fieldId: string,
    field: "workspaceId" | "orgUnitIds",
    value: string | string[],
    workspaceId?: string
  ) => void;
}

const UserWorkspaceAssociationFieldComponent: React.FC<UserWorkspaceAssociationFieldProps> = ({
  fieldId,
  selectedOrgUnits,
  workspaceList,
  workspaceId,
  hasOnlyOneSelection,
  deleteWorkspaceSelection,
  handleWorkspaceOUSelection,
  selectedWorkspaceIds
}) => {
  const [orgList, setOrgList] = useState<Array<any>>([]);
  const dispatch = useDispatch();
  const OUsForWorkspaceState = useParamSelector(getOUsForWorkspace, { workspaceId });
  const orgUnitEnhancementSupport = useHasEntitlements(Entitlement.ORG_UNIT_ENHANCEMENTS, EntitlementCheckType.AND);
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

  const orgDropdownSelection = useMemo(() => {
    return orgList.map((org: any) => {
      return (
        <Select.Option value={org.id} key={org.id} label={org.name}>
          {org.name}
        </Select.Option>
      );
    });
  }, [orgList]);

  const selectedActiveOrgs = useMemo(() => {
    if (orgList?.length > 0) {
      return arrayOfObjFilterAndMap(orgList, selectedOrgUnits);
    }
  }, [orgList, selectedOrgUnits]);

  const treeList = useMemo(() => {
    return cloneDeep(createDataTree(orgList, false));
  }, [orgList]);

  const PopupTreeRender = useMemo(() => {
    return (
      <span>
        <OrgTree
          key={fieldId}
          dataSource={treeList}
          onCheck={rows => {
            handleWorkspaceOUSelection(fieldId, "orgUnitIds", rows);
          }}
          notFoundContent={OUsForWorkspaceState.isloading ? <Loader /> : null}
          selectedRowsKeys={selectedActiveOrgs}
          label="name"
          flatList={orgList}
        />
      </span>
    );
  }, [fieldId, treeList, orgList, selectedActiveOrgs, handleWorkspaceOUSelection]);

  const orgDropdown = useMemo(() => {
    return (
      <div className="dev-score-profile-container-section-container-header w-40">
        <AntText className="section-header">COLLECTION</AntText>
        <AntText className="section-sub-header">Select the Collection for this Account.</AntText>
        {orgUnitEnhancementSupport ? (
          PopupTreeRender
        ) : (
          <Select
            className="selector"
            key="OrgUnit"
            mode="multiple"
            value={selectedActiveOrgs}
            placeholder={"Select Collections"}
            disabled={!workspaceId}
            onChange={(values: string[]) => handleWorkspaceOUSelection(fieldId, "orgUnitIds", values)}
            notFoundContent={OUsForWorkspaceState.isloading ? <Loader /> : null}
            filterOption={(input: any, option: any) => option.props.label.toLowerCase().includes(input.toLowerCase())}>
            {orgDropdownSelection}
          </Select>
        )}
      </div>
    );
  }, [
    fieldId,
    workspaceId,
    orgDropdownSelection,
    handleWorkspaceOUSelection,
    selectedOrgUnits,
    OUsForWorkspaceState.isloading,
    selectedActiveOrgs,
    treeList
  ]);

  const workspaceDropdownSelection = useMemo(() => {
    return workspaceList
      .filter(
        (workspace: WorkspaceModel) => workspaceId === workspace.id || !selectedWorkspaceIds.includes(workspace.id)
      )
      .map((workspace: WorkspaceModel) => {
        return (
          <Select.Option value={workspace.id} key={workspace.id} label={workspace.name}>
            {workspace.name}
          </Select.Option>
        );
      });
  }, [fieldId, workspaceId, workspaceList, selectedWorkspaceIds]);

  const workspaceDropdown = useMemo(
    () => (
      <div className="dev-score-profile-container-section-container-header w-40 mr-80">
        <AntText className="section-header">PROJECT</AntText>
        <AntText className="section-sub-header">Select a project to view available Collection.</AntText>
        <AntSelect
          showSearch
          className="user-workspace-selector"
          key="workspace"
          value={workspaceId}
          placeholder={"Select Project"}
          onChange={(value: string) => handleWorkspaceOUSelection(fieldId, "workspaceId", value)}
          filterOption={(input: any, option: any) => option.props.label.toLowerCase().includes(input.toLowerCase())}>
          {workspaceDropdownSelection}
        </AntSelect>
      </div>
    ),
    [fieldId, workspaceId, workspaceList, handleWorkspaceOUSelection]
  );

  return (
    <div className="flex py-20" key={fieldId}>
      {workspaceDropdown}
      {orgDropdown}
      <Popconfirm
        key={"delete-action"}
        title={"Do you want to delete this item?"}
        onConfirm={e => deleteWorkspaceSelection(fieldId)}
        okText={"Yes"}
        cancelText={"No"}>
        <AntButton icon="delete" className="tree-delete-btn margin-center" disabled={hasOnlyOneSelection} />
      </Popconfirm>
    </div>
  );
};

export default UserWorkspaceAssociationFieldComponent;
