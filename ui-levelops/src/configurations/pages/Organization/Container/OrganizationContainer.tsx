import React, { useEffect, useMemo, useState } from "react";
import queryString from "query-string";
import { RouteComponentProps, useHistory, useLocation, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { getBreadcrumbForOrganizationPage } from "../Helpers/getBreadCrumbForOrganizationPage";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import OrganizationUnitLandingPage from "../organization-unit/container/OrganizationUnitLandingPage";
import { orgUnitPivotsList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { PIVOT_LIST_ID } from "../Constants";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { PivotType } from "configurations/configuration-types/OUTypes";
import { get } from "lodash";
import { Spin } from "antd";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import {
  getGenericPageButtonTypeSelector,
  getGenericPageLocationSelector
} from "reduxConfigs/selectors/pagesettings.selector";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import {
  sessionUserMangedOURefs,
  sessionUserWorkspacesSelections
} from "reduxConfigs/selectors/session_current_user.selector";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import { useViewCollectionPermission } from "custom-hooks/HarnessPermissions/useViewCollectionPermission";
import { ProjectPathProps } from "classes/routeInterface";
import { getIsStandaloneApp } from "helper/helper";
import { useAppStore } from "contexts/AppStoreContext";
import { useWorkspace } from "custom-hooks/useWorkspace";

interface OrganizationContainerProps extends RouteComponentProps {
  rbacOUs?: number[];
}

const OrganizationContainer: React.FC<OrganizationContainerProps> = props => {
  const [tabs, setTabs] = useState<Array<PivotType>>([]);
  const [pivotsLoading, setPivotsLoading] = useState<boolean>(false);
  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots_list",
    method: "list",
    uuid: PIVOT_LIST_ID
  });
  const getSelectedWorkspaceState = useSelector(getSelectedWorkspace);
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const hasViewCollecionAccess = useViewCollectionPermission();
  const isStandaloneApp = getIsStandaloneApp();
  const isAdmin = isStandaloneApp ? userRole?.toLowerCase() === USERROLES.ADMIN : hasViewCollecionAccess;

  let { ou_workspace_id } = queryString.parse(location.search);
  const pageSelectorState = useParamSelector(getGenericPageButtonTypeSelector, {
    location: props?.history?.location?.pathname,
    buttonType: "select_dropdown"
  });

  const { workSpaceListData, loading } = useWorkSpaceList(!isAdmin);
  const allowedWorkspaces: Record<string, string[]> = useSelector(sessionUserWorkspacesSelections);
  const allowedOus: Record<string, string[]> = useSelector(sessionUserMangedOURefs);
  const projectParams = useParams<ProjectPathProps>();

  const pagePathnameState = useParamSelector(getGenericPageLocationSelector, {
    location: props?.history?.location?.pathname
  });

  const { selectedProject, accountInfo } = useAppStore();
  const { identifier: accountId = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
  const { isFetching, workspaceId } = useWorkspace({
    accountId,
    orgIdentifier,
    projectIdentifier
  });

  const fetchPivotsList = (workspace_id = ou_workspace_id) => {
    setTabs([]);
    const filters = sanitizeObjectCompletely({ filter: { workspace_id: [workspace_id] } });
    setPivotsLoading(true);
    dispatch(restapiClear("pivots_list", "list", PIVOT_LIST_ID));
    dispatch(orgUnitPivotsList(PIVOT_LIST_ID, filters));
  };

  useEffect(() => {
    if (!isStandaloneApp && workspaceId) {
      fetchPivotsList(workspaceId);
    }
  }, [workspaceId]);

  const resetTabs = (workspaceId: string) => {
    if (isStandaloneApp) {
      /** setting new workspace for ou workspace selection */
      fetchPivotsList(workspaceId);
      history.push({
        search: `?ou_workspace_id=${workspaceId}`
      });
    }
  };

  useEffect(() => {
    /** setting ou workspace selection
     * 1. If current workspace exits then set it
     * 2. If current workspace does not exits set the first workspace as selected
     */
    if (!ou_workspace_id) {
      const selectedWorkspaceId = get(getSelectedWorkspaceState, ["id"]);
      const allowedWorkspacesKeys = Object.keys(allowedWorkspaces);
      if (
        (allowedWorkspacesKeys.length && allowedWorkspacesKeys.includes(selectedWorkspaceId)) ||
        (!allowedWorkspacesKeys.length && selectedWorkspaceId)
      ) {
        resetTabs(selectedWorkspaceId);
      } else if ((workSpaceListData ?? []).length) {
        resetTabs(workSpaceListData[0].id);
      }
    }
  }, [workSpaceListData, getSelectedWorkspaceState, ou_workspace_id, allowedWorkspaces]);

  useEffect(() => {
    if (ou_workspace_id && isStandaloneApp) {
      const allowedWorkspacesKeys = Object.keys(allowedWorkspaces);
      if (!allowedWorkspacesKeys.length || allowedWorkspacesKeys.includes(ou_workspace_id as string)) {
        fetchPivotsList();
      } else if (!allowedWorkspacesKeys.includes(ou_workspace_id as string)) {
        resetTabs(allowedWorkspacesKeys[0]);
      }
    }
    return () => {
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, []);

  useEffect(() => {
    if (pivotsLoading) {
      const loading = get(pivotsListState, ["loading"], true);
      const error = get(pivotsListState, ["error"], true);
      if (!loading) {
        if (!error) {
          let records: Array<PivotType> = get(pivotsListState, ["data", "records"], []);
          const hasPivotAccess = isStandaloneApp && getRBACPermission(PermeableMetrics.ORG_PIVOT_SHOW);
          if (hasPivotAccess) {
            records = records.filter(pivot => pivot.enabled);
          }
          const accessibleOUs = isStandaloneApp ? (!isAdmin ? allowedOus || [] : undefined) : props.rbacOUs;
          if (accessibleOUs) {
            records = records.reduce((acc, pivot) => {
              // @ts-ignore
              const newCount = accessibleOUs?.filter((currentUsrOU: number) => {
                return pivot?.ou_ref_ids?.indexOf(currentUsrOU) !== -1;
              });
              pivot.count_of_ous = newCount.length;
              if (pivot.count_of_ous) {
                // @ts-ignore
                acc.push(pivot);
              }
              return acc;
            }, []);
          }
          setTabs(records);
        }
        setPivotsLoading(false);
      }
    }
  }, [pivotsListState, pivotsLoading, props.rbacOUs]);

  useEffect(() => {
    if (isStandaloneApp && !loading && workSpaceListData?.length) {
      dispatch(
        setPageSettings(props.location.pathname, {
          title: "Collections",
          action_buttons: {},
          withBackButton: true,
          bread_crumbs: getBreadcrumbForOrganizationPage(projectParams),
          bread_crumbs_position: "before",
          select_dropdown: {
            workSpace: {
              label: "Project:",
              options: (workSpaceListData || []).map((item: any) => ({
                value: item?.id,
                label: item?.name
              })),
              selected_option: { value: ou_workspace_id } || ""
            }
          }
        })
      );
    } else {
      dispatch(
        setPageSettings(props.location.pathname, {
          title: "Collections",
          action_buttons: {},
          withBackButton: true,
          bread_crumbs: getBreadcrumbForOrganizationPage(projectParams),
          bread_crumbs_position: "before"
        })
      );
    }
  }, [workSpaceListData, loading, getSelectedWorkspaceState, ou_workspace_id]);

  useEffect(() => {
    const hasclicked = get(pageSelectorState, ["workSpace", "hasClicked"], false);
    if (hasclicked) {
      const id = get(pageSelectorState, ["workSpace", "value"], "");
      resetTabs(id);
      dispatch(
        setPageSettings(props.location.pathname, {
          ...pagePathnameState,
          select_dropdown: {
            workSpace: {
              ...pageSelectorState?.["workSpace"],
              hasClicked: false
            }
          }
        })
      );
    }
  }, [pageSelectorState]);

  const currentWorkspace = useMemo(
    () => workSpaceListData?.find(workspace => workspace?.id?.toString() === ou_workspace_id),
    [ou_workspace_id, workSpaceListData]
  );

  if (pivotsLoading || !ou_workspace_id) {
    return (
      <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
        <Spin />
      </div>
    );
  }

  return (
    <OrganizationUnitLandingPage
      tabs={tabs}
      fetchPivotsList={fetchPivotsList}
      currentWorkspaceIsDemo={isStandaloneApp && !!currentWorkspace?.demo}
      isAdmin={isAdmin}
    />
  );
};
export default OrganizationContainer;
