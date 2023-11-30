import React, { useCallback, useMemo, useRef, useState } from "react";
import { Select } from "antd";
import queryString from "query-string";
import { getHomePage } from "constants/routePaths";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { isEqual } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { WebRoutes } from "routes/WebRoutes";
import { AntButton, AntIcon, AntInput, AntSelect, AntText, AntTooltip } from "shared-resources/components";
import { truncateAndEllipsis } from "utils/stringUtils";
import "./workspaceSelectDropdown.styles.scss";
import { RestUsers } from "classes/RestUsers";
import { userProfileUpdate } from "reduxConfigs/actions/restapi";
import { sessionCurrentUser } from "reduxConfigs/actions/sessionActions";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { ProjectPathProps } from "@harness/microfrontends/dist/modules/10-common/interfaces/RouteInterfaces";

interface WorkspaceSelectDropdownProps {
  workspaces: Array<WorkspaceModel>;
  className?: string;
  loading: boolean;
}

const { Option } = Select;
const WorkspaceSelectDropdownComponent: React.FC<WorkspaceSelectDropdownProps> = (
  props: WorkspaceSelectDropdownProps
) => {
  const { workspaces, loading, className } = props;
  const [searchedWorkspace, setSearchedWorkspace] = useState<string>("");
  const [dropdownVisible, setDropdownVisible] = useState<boolean>(false);
  const selectedWorkspace: WorkspaceModel = useSelector(getSelectedWorkspace);
  const sessionCurrentUserData = useSelector(sessionCurrentUserState);
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const mouseLeaveRef = useRef<boolean>(true);
  const workspaceEditRef = useRef<boolean>(false);
  const { workspace_id } = queryString.parse(location.search);
  const projectParams = useParams<ProjectPathProps>();

  const handleWorkspaceChange = (value: string) => {
    const workspaceSelected: WorkspaceModel | undefined = (workspaces ?? []).find(workspace => workspace.id === value);
    if (workspaceSelected && !workspaceEditRef.current) {
      dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, workspaceSelected ?? {}));
      /** updating selected workspace in BE */
      const sessionCurrentData = {
        ...sessionCurrentUserData,
        metadata: {
          ...sessionCurrentUserData?.metadata,
          selected_workspace: workspaceSelected
        }
      };
      const restUser = new RestUsers({
        ...sessionCurrentData
      });
      dispatch(userProfileUpdate(restUser));
      dispatch(sessionCurrentUser({ loading: false, error: false, data: sessionCurrentData }));
      const currentURL = location.pathname?.concat(location?.search);
      if (!isEqual(currentURL, getHomePage(projectParams))) {
        history.push(getHomePage(projectParams));
      }
    }
    setSearchedWorkspace("");
    setDropdownVisible(false);
  };

  const handleSearchWorkspaceChange = (e: { target: { value: string } }) => {
    setSearchedWorkspace(e.target.value);
  };

  const handleDropdownMouseLeave = () => {
    mouseLeaveRef.current = true;
  };

  const handleManageWorkspace = () => {
    history.push(WebRoutes.workspace.root());
  };

  const handleWorkspaceEdit = (workspaceId: string) => {
    workspaceEditRef.current = true;
    history.push(WebRoutes.workspace.edit(workspaceId));
  };

  const renderDropdown = useCallback(
    menu => (
      <div onMouseLeave={handleDropdownMouseLeave}>
        <AntInput placeHolder="search" onChange={handleSearchWorkspaceChange} value={searchedWorkspace} />
        {menu}
        {getRBACPermission(PermeableMetrics.MANAGE_WORKSPACE) && (
          <div className="manage-workspace-container">
            <AntButton type="primary" onClick={!selectedWorkspace?.demo ? handleManageWorkspace : () => {}}>
              Manage Projects
            </AntButton>
          </div>
        )}
      </div>
    ),
    [searchedWorkspace]
  );

  const filteredWorkspaces = useMemo(
    () =>
      (workspaces ?? []).filter(workspace =>
        workspace?.name?.toLowerCase().includes((searchedWorkspace ?? "")?.toLowerCase())
      ),
    [searchedWorkspace, workspaces]
  );

  const onDropdownVisibleChange = (open: any) => {
    if (!open && !mouseLeaveRef.current) {
      return;
    } else if (open) {
      mouseLeaveRef.current = false;
    }
    if (!open && mouseLeaveRef.current) {
      setSearchedWorkspace("");
    }
    setDropdownVisible(open);
  };

  const renderLabel = useCallback((label: string) => {
    if (label) {
      const MAX_LABEL_LENGTH = 15;
      if (label.length > MAX_LABEL_LENGTH) {
        return <AntTooltip title={label}>{truncateAndEllipsis(label, MAX_LABEL_LENGTH)}</AntTooltip>;
      }
      return label;
    }
    return "";
  }, []);

  return (
    <div className={className ?? "workspace-select-container"}>
      <AntText className="title">Project :</AntText>
      <AntSelect
        className="workspace-dropdown-container"
        value={workspace_id ?? selectedWorkspace?.id}
        onChange={handleWorkspaceChange}
        loading={loading}
        onDropdownVisibleChange={onDropdownVisibleChange}
        open={dropdownVisible}
        dropdownClassName="workspace-dropdown"
        dropdownRender={renderDropdown}>
        {(filteredWorkspaces ?? []).map((workspace: WorkspaceModel) => (
          <Option value={workspace?.id} className="workspace-option" label={workspace?.name} key={workspace?.id}>
            <AntText>{renderLabel(workspace?.name)}</AntText>
            <AntIcon type="setting" onClick={e => handleWorkspaceEdit(workspace?.id)} />
          </Option>
        ))}
      </AntSelect>
    </div>
  );
};

export default WorkspaceSelectDropdownComponent;
