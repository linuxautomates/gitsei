import * as React from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { debounce, get } from "lodash";
import queryString from "query-string";
import "./dashboard.components.scss";
import { Badge, Button, Empty, notification, Popover, Tooltip } from "antd";
import { AntTitle, EditCloneModal } from "../../../../shared-resources/components";
import {
  dashboardDefault,
  dashboardsCreate,
  dashboardsDelete,
  dashboardsGet,
  dashboardsList
} from "reduxConfigs/actions/restapi";
import Loader from "components/Loader/Loader";
import AntIconComponent from "../../../../shared-resources/components/ant-icon/ant-icon.component";
import { cloneWidgets } from "dashboard/helpers/helper";
import { DASHBOARD_ROUTES, getBaseUrl } from "constants/routePaths";
import { SearchInput } from "./SearchInput.component";
import DashboardRow from "./DashboardRow";
import {
  _dashboardsCreateSelector,
  _dashboardsDeleteSelector,
  _dashboardsGetSelector,
  _dashboardsListSelector,
  _dashboardsUpdateSelector,
  dashboardsCreateSelector
} from "reduxConfigs/selectors/dashboardSelector";
import { DEFAULT_DASHBOARD_KEY } from "../../../constants/constants";
import { newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidgetsByDashboardId } from "reduxConfigs/selectors/widgetSelector";
import { RestWidget } from "../../../../classes/RestDashboards";
import { RBAC } from "constants/localStorageKeys";
import ButtonGroup from "antd/lib/button/button-group";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { useLocation, useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";
import { useIsDashboardReadonly } from "custom-hooks/HarnessPermissions/useIsDashboardReadonly";

const DROPDOWN_DASH = "DROPDOWN_DASH";
const SEARCH_DASH = "SEARCH_DASH";

interface DashboardSearchDropdownProps {
  setAsDefault: (id: string) => void;
  dashCount: number;
  currentDashId: string;
  history: any;
  dashboardTitle: string;
}

export const DashboardSearchDropdown: React.FC<DashboardSearchDropdownProps> = props => {
  const dispatch = useDispatch();

  const dashboardListState = useSelector(_dashboardsListSelector);
  const dashboardDeleteState = useSelector(_dashboardsDeleteSelector);
  const dashboardDetailState = useSelector(_dashboardsGetSelector);
  const createState = useSelector(dashboardsCreateSelector);
  const updateState = useSelector(_dashboardsUpdateSelector);
  const [searchValue, setSearchValue] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [list, setList] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [cloning, setCloning] = useState(false);
  const [actionDashId, setActionDashId] = useState<undefined | string>(undefined);
  const [refresh, setRefresh] = useState(0);
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [showCloneModal, setShowCloneModal] = useState(false);
  const [cloneName, setCloneName] = useState<undefined | string>(undefined);
  const [updatingDefault, setUpdatingDefault] = useState(false);
  const [showPopover, setShowPopover] = useState(false);
  const [nameSearching, setNameSearching] = useState(false);
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [searchName, setSearchName] = useState<undefined | string>(undefined);
  const entDashboard = useHasEntitlements([Entitlement.DASHBOARDS]);

  const widgets = useParamSelector(getWidgetsByDashboardId, { dashboard_id: actionDashId });

  const dashboardCountExceed = useHasEntitlements(Entitlement.DASHBOARDS_COUNT_3, EntitlementCheckType.AND);
  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const location = useLocation();
  const { workspace_id } = queryString.parse(location.search);
  const projectParams = useParams<ProjectPathProps>();

  const dashboardListSearch = (value?: string) => {
    setSearching(true);
    dispatch(
      dashboardsList(
        {
          page_size: 1000,
          filter: {
            partial: { name: value || "" },
            workspace_id: !!selectedWorkspace?.id ? parseInt(selectedWorkspace?.id) : ""
          }
        },
        DROPDOWN_DASH
      )
    );
  };

  useEffect(() => {
    if (!searching) {
      dashboardListSearch(searchValue);
    }
  }, [refresh]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    // get(dashboardListState, [DROPDOWN_DASH, "data", "records"], []).length !== list.length) condition is added to handle a situation when last API response is ignored
    // because searching is set false from the second last [Race condition]
    if (
      dashboardListState &&
      (searching || loading || get(dashboardListState, [DROPDOWN_DASH, "data", "records"], []).length !== list.length)
    ) {
      const { loading: _loading, error } = get(dashboardListState, [DROPDOWN_DASH], { loading: true, error: true });
      if (_loading !== undefined && !_loading && error !== undefined && !error) {
        setLoading(false);
        setList(get(dashboardListState, [DROPDOWN_DASH, "data", "records"], []));
        setSearching(false);
      }
    }

    if (dashboardListState && nameSearching) {
      const { loading: _loading, error } = get(dashboardListState, [SEARCH_DASH], { loading: true, error: true });
      if (_loading !== undefined && !_loading && error !== undefined && !error) {
        setNameSearching(false);
        const data = get(dashboardListState, [SEARCH_DASH, "data", "records"], []);
        setNameExist(
          !!data?.filter((item: any) => item?.name?.toLowerCase() === searchName?.toLowerCase())?.length || false
        );
      }
    }
  }, [dashboardListState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (dashboardLoading && dashboardDetailState && actionDashId) {
      const { loading: _loading, error } = get(dashboardDetailState, [actionDashId], { loading: true, error: true });
      if (!_loading && !error) {
        const data = get(dashboardDetailState, [actionDashId, "data"], {});
        if (cloneName) {
          const newDashboard = {
            ...data,
            widgets: cloneWidgets(widgets.map((w: RestWidget) => w.json)),
            name: cloneName,
            default: false
          };
          dispatch(dashboardsCreate(newDashboard));
          setCloning(true);
        } else {
          const updatedDashboard = {
            ...data,
            default: true
          };
          dispatch(newDashboardUpdate(actionDashId, updatedDashboard));
          setUpdatingDefault(true);
        }
        setDashboardLoading(false);
      }
    }
  }, [dashboardDetailState, widgets]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (updatingDefault && actionDashId) {
      const { loading: _loading, error } = get(updateState, [actionDashId], { loading: true, error: true });
      if (!_loading && !error) {
        props.setAsDefault(actionDashId as string);
        setUpdatingDefault(false);
        setRefresh(state => state + 1);
        setActionDashId(undefined);
        notification.success({
          message: "Successfully Updated",
          description: "Default insight successfully updated"
        });
      }
    }
  }, [updateState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (cloning && actionDashId) {
      const { loading: _loading, error } = get(createState, ["0"], { loading: true, error: true });
      if (!_loading && !error) {
        const data = get(createState, ["0", "data"], {});
        setCloning(false);
        setRefresh(state => state + 1);
        setActionDashId(undefined);
        props.history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.LIST}/${data.id}`);
      }
    }
  }, [createState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (deleting && dashboardDeleteState && actionDashId) {
      const { loading, error } = get(dashboardDeleteState, [actionDashId], { loading: true, error: true });
      if (!loading && !error) {
        dispatch(dashboardDefault(DEFAULT_DASHBOARD_KEY));
        if (actionDashId === props.currentDashId) {
          props.history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.LIST}`);
        }
        setRefresh(state => state + 1);
        setActionDashId(undefined);
      }
    }
  }, [dashboardDeleteState]); // eslint-disable-line react-hooks/exhaustive-deps

  const memoizedDashListStyle = useMemo(() => ({ minHeight: "65vh", maxHeight: "65vh", overflowY: "auto" }), []);

  const handleClone = useCallback((dashboardId: string) => {
    setShowCloneModal(true);
    setShowPopover(false);
    setActionDashId(dashboardId);
  }, []);

  const handleDelete = useCallback((dashboardId: string) => {
    setDeleting(true);
    setActionDashId(dashboardId);
    dispatch(dashboardsDelete(dashboardId));
  }, []);

  const handleDefault = useCallback((dashboardId: string) => {
    setDashboardLoading(true);
    setActionDashId(dashboardId);
    dispatch(dashboardsGet(dashboardId));
  }, []);

  const dashList = useMemo(() => {
    return (
      // @ts-ignore
      <div style={memoizedDashListStyle}>
        {list.map((item: any, index: number) => (
          <DashboardRow
            history={props.history}
            key={`row-${index}`}
            dashboard={item}
            actionDashId={actionDashId}
            onCloneClick={handleClone}
            onDeleteClick={handleDelete}
            onDefaultClick={handleDefault}
          />
        ))}
        {!list.length && <Empty style={{ marginTop: "24vh" }} />}
      </div>
    );
  }, [list, actionDashId]);

  const memoizedStyle = useMemo(() => ({ width: "23.5rem" }), []);
  const memoizedBtnStyle = useMemo(() => ({ width: "100%", marginTop: "1rem", borderRadius: 0 }), []);

  const debouncedSearch = useCallback(
    debounce((v: string) => {
      dashboardListSearch(v);
      setSearchValue(v);
    }, 250),
    []
  );

  const handleSearchChange = useCallback((v: string) => {
    debouncedSearch(v);
  }, []);

  const handleAddDashboard = useCallback(
    () => props.history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.CREATE}`),
    []
  );
  const handleManageDashboard = useCallback(
    () => props.history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.LIST}`),
    []
  );

  const hasHarnessEditAccess = useIsDashboardReadonly();

  const popupContent = useMemo(() => {
    if (loading) {
      return <Loader />;
    }
    const rbac = localStorage.getItem(RBAC);
    return (
      <div style={memoizedStyle}>
        <SearchInput onChange={handleSearchChange} loading={searching} />
        {dashList}
        {(window.isStandaloneApp
          ? getRBACPermission(PermeableMetrics.DASHBOARD_SEARCH_DROPDOWN_ACTIONS)
          : hasHarnessEditAccess) && (
          <ButtonGroup className="dashboard-popup-btn-grp">
            <Tooltip
              style={memoizedBtnStyle}
              title={dashboardCountExceed || !entDashboard ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
              <Button
                className={`${dashboardCountExceed ? "add-dashboard-disabled" : "add-dashboard"}`}
                disabled={dashboardCountExceed || !entDashboard}
                style={dashboardCountExceed || !entDashboard ? { width: "100%" } : memoizedBtnStyle}
                type="primary"
                onClick={handleAddDashboard}>
                Add Dashboard
              </Button>
            </Tooltip>
            <Tooltip style={memoizedBtnStyle} title={!entDashboard ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
              <Button
                className={`${!entDashboard ? "manage-dashboard-disabled" : "manage-dashboard"}`}
                disabled={!entDashboard}
                style={!entDashboard ? { width: "100%" } : memoizedBtnStyle}
                type="primary"
                onClick={handleManageDashboard}>
                Manage Insights
              </Button>
            </Tooltip>
          </ButtonGroup>
        )}
      </div>
    );
  }, [searching, dashList, entDashboard]);

  const handleVisibleChange = useCallback(visible => setShowPopover(visible), []);

  const ddButton = useMemo(
    () => (
      <Button type={"ghost"} className="dashboard-title-button flex align-center" style={{ paddingLeft: 0 }}>
        <AntTitle level={4} style={{ margin: 0 }}>
          {props.dashboardTitle ? props.dashboardTitle : "Insights"}
        </AntTitle>
        {props.dashCount >= 0 && (
          <Badge
            style={{
              margin: "0 0.5rem",
              backgroundColor: "var(--harness-blue)"
            }}
            count={props.dashCount}
            overflowCount={1000}
          />
        )}
        <AntIconComponent type="down" />
      </Button>
    ),
    [props.dashboardTitle, props.dashCount]
  );

  const debouncedCloneNameSearch = useCallback(
    debounce((value: string) => {
      dispatch(
        dashboardsList(
          {
            filter: {
              partial: { name: value },
              workspace_id:
                !!workspace_id || !!selectedWorkspace?.id ? parseInt(workspace_id ?? selectedWorkspace?.id) : ""
            }
          },
          SEARCH_DASH
        )
      );
      setNameSearching(true);
    }, 250),
    [selectedWorkspace, workspace_id]
  );

  const handleCloneNameSeach = useCallback((value: string) => {
    setSearchName(value);
    debouncedCloneNameSearch(value);
  }, []);

  return (
    <div style={{ display: "flex", alignItems: "center" }}>
      <Popover
        className={"search-popover"}
        placement={"bottomLeft"}
        content={popupContent}
        trigger="click"
        visible={showPopover}
        onVisibleChange={handleVisibleChange}
        align={{
          overflow: { adjustX: false, adjustY: false }
        }}
        // @ts-ignore
        getPopupContainer={trigger => trigger.parentNode}>
        {ddButton}
      </Popover>
      {showCloneModal && (
        <EditCloneModal
          visible={showCloneModal}
          title={"Clone Insight"}
          onOk={(name: string) => {
            setShowCloneModal(false);
            setCloneName(name);
            dispatch(dashboardsGet(actionDashId));
            setDashboardLoading(true);
            setShowPopover(true);
          }}
          onCancel={() => {
            setShowCloneModal(false);
            setActionDashId(undefined);
            setShowPopover(true);
          }}
          searchEvent={handleCloneNameSeach}
          nameExists={nameExist}
        />
      )}
    </div>
  );
};
