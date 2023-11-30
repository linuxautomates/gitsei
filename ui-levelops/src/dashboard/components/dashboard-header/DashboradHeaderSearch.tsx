import * as React from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { debounce, get, parseInt } from "lodash";
import queryString from "query-string";
import { Button, Empty, Popover, Tooltip, Breadcrumb, notification } from "antd";
import { AntButton, AntCol, AntRow, AntTooltip, EditCloneModal } from "shared-resources/components";
import Loader from "components/Loader/Loader";
import AntIconComponent from "shared-resources/components/ant-icon/ant-icon.component";
import { getDashboardsPage, DASHBOARD_ROUTES, getHomePage, getBaseUrl } from "constants/routePaths";
import {
  _dashboardsCreateSelector,
  _dashboardsDeleteSelector,
  _dashboardsGetSelector,
  _dashboardsListSelector,
  _dashboardsUpdateSelector,
  selectedDashboard,
  dashboardsCreateSelector
} from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { widgetsSelector } from "reduxConfigs/selectors/widgetSelector";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import ButtonGroup from "antd/lib/button/button-group";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import {
  DASHBOARD_ELIPSIS_LENGTH,
  DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH,
  OU_DASHBOARD_LIST_ID,
  OU_DASHBOARD_SEARCH_LIST_ID
} from "configurations/pages/Organization/Constants";
import { orgUnitDashboardList, ouDashboardSetDefault } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import {
  dashboardsCreate,
  dashboardsDelete,
  dashboardsGet,
  dashboardsList,
  restapiClear
} from "reduxConfigs/actions/restapi";
import {
  getSelectedOU,
  orgUnitGetDataSelect,
  orgUnitUpdateState
} from "reduxConfigs/selectors/OrganizationUnitSelectors";
import DashboardSearchPopoverActions from "./DashboardSearchPopoverActions";
import { nestedSort, shiftArrayByKey } from "utils/arrayUtils";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { cloneWidgets, filteredDeprecatedWidgets } from "dashboard/helpers/helper";
import { DASHBOARD_CREATE_PIVOT_UUID } from "dashboard/constants/uuid.constants";
import { OUCategoryOptionsType, PivotType } from "configurations/configuration-types/OUTypes";
import { WebRoutes } from "routes/WebRoutes";
import widgetConstants from "dashboard/constants/widgetConstants";
import "./dashboardHeaderSearch.styles.scss";
import { ProjectPathProps } from "classes/routeInterface";
import { getIsStandaloneApp } from "helper/helper";

const SEARCH_DASH = "SEARCH_DASH";

interface DashboradHeaderSearchProps {
  ouId?: string;
}

export const DashboradHeaderSearch: React.FC<DashboradHeaderSearchProps> = props => {
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const [searchValue, setSearchValue] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [list, setList] = useState<Array<RestDashboard>>([]);
  const [searching, setSearching] = useState(false);
  const [actionDashId, setActionDashId] = useState<undefined | number | string>(undefined);
  const [showPopover, setShowPopover] = useState(false);
  const [nameSearching, setNameSearching] = useState(false);
  const [searchName, setSearchName] = useState<undefined | string>(undefined);
  const entDashboard = useHasEntitlements([Entitlement.DASHBOARDS]);
  const [topDashboards, setTopDashboards] = useState<any[]>([]);
  const params = queryString.parse(location.search) as any;
  const widgets = useSelector(widgetsSelector) as RestWidget;
  const [cloneDashPivotList, setPivots] = useState<Array<OUCategoryOptionsType>>([]);
  const selectedDash = useSelector(selectedDashboard) as RestDashboard;
  const [defaultLoadingDashboard, setDefaultLoading] = useState<string | undefined>(undefined);
  const [showCloneModal, setShowCloneModal] = useState(false);
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [cloneData, setCloneData] = useState<any>(undefined);
  const projectParams = useParams<ProjectPathProps>();

  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const isSamePath = location.pathname === `${getDashboardsPage(projectParams)}/${selectedDash?.id}`;

  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const dashboardDetailState = useSelector(_dashboardsGetSelector);
  const createState = useSelector(dashboardsCreateSelector);
  const dashboardDeleteState = useSelector(_dashboardsDeleteSelector);
  const selectedOUState = useSelector(getSelectedOU);
  const dashboardListSearch = (value?: string) => {
    let stateRefId = SEARCH_DASH;
    setNameSearching(true);
    if (!value) {
      setNameSearching(false);
      stateRefId = OU_DASHBOARD_SEARCH_LIST_ID;
    }
    // dispatch(dashboardsList({ page_size: 1000, filter: { partial: { name: value || "" } } }, DROPDOWN_DASH));
    dispatch(
      orgUnitDashboardList(stateRefId, {
        ou_id: props.ouId,
        partial: { name: value || "" },
        inherited: true
      })
    );
  };

  const dashboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_dashboard_list",
    method: "list",
    uuid: OU_DASHBOARD_SEARCH_LIST_ID
  });
  const dashboardListSearchState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_dashboard_list",
    method: "list",
    uuid: SEARCH_DASH
  });

  const pivots = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots_list",
    method: "list",
    uuid: DASHBOARD_CREATE_PIVOT_UUID
  });

  const orgUnitGetState = useParamSelector(orgUnitGetDataSelect, {
    id: params?.OU
  });

  const orgUnitUpdateStates = useParamSelector(orgUnitUpdateState, {
    id: params?.OU
  });

  const selectedDashboardWidgetsState = useSelector(widgetsSelector);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("org_dashboard_list", "list", OU_DASHBOARD_LIST_ID));
      dispatch(restapiClear("org_dashboard_list", "list", SEARCH_DASH));
      dispatch(restapiClear("org_dashboard_list", "list", OU_DASHBOARD_SEARCH_LIST_ID));
    };
  }, []);

  useEffect(() => {
    const loading = get(pivots, ["loading"], true);
    const error = get(pivots, ["error"], true);
    if (!loading) {
      let nrecords: Array<OUCategoryOptionsType> = [];
      if (!error) {
        let records: Array<PivotType> = get(pivots, ["data", "records"], []);
        nrecords = records.reduce((acc: any, curValue: PivotType) => {
          if (curValue.enabled) {
            acc.push(curValue);
          }
          return acc;
        }, []);
        setPivots(nrecords);
      }
    }
  }, [pivots]);

  useEffect(() => {
    const loading = get(orgUnitUpdateStates, ["loading"], true);
    const error = get(orgUnitUpdateStates, ["error"], true);
    const data = get(orgUnitUpdateStates, ["data"], "");
    if (data === "ok" && !loading) {
      setDefaultLoading(undefined);
      if (error) {
        notification.error({ message: "Failed to set default insight" });
      }
    }
  }, [orgUnitUpdateStates]);

  useEffect(() => {
    if (props?.ouId) {
      const isRoot = selectedOUState?.parent_ref_id ? true : false;
      dispatch(orgUnitDashboardList(OU_DASHBOARD_SEARCH_LIST_ID, { ou_id: props?.ouId, inherited: isRoot }));
    }
  }, [props.ouId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (dashboardListSearchState && nameSearching) {
      const loading = get(dashboardListSearchState, ["loading"], true);
      const error = get(dashboardListSearchState, ["error"], true);
      if (loading !== undefined && !loading && error !== undefined && !error) {
        // setNameSearching(false);
        const data = get(dashboardListSearchState, ["data", "records"], []);
        setList(data);
      }
    }
  }, [dashboardListSearchState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    // get(dashboardListState, [DROPDOWN_DASH, "data", "records"], []).length !== list.length) condition is added to handle a situation when last API response is ignored
    // because searching is set false from the second last [Race condition]
    let nlist = get(dashboardListState, ["data", "records"], []);
    if (dashboardListState && (searching || loading || nlist.length !== list.length) && isSamePath) {
      const loading = get(dashboardListState, ["loading"], true);
      const error = get(dashboardListState, ["error"], true);
      if (loading !== undefined && !loading && error !== undefined && !error) {
        if (nlist.length === 0) {
          history.push({
            pathname: `${getBaseUrl()}/no-ou-dash`,
            search: location.search
          });
        }
        nlist = shiftArrayByKey(nestedSort(nlist, "ou_id", "dashboard_order"), "ou_id", props.ouId || undefined) || [];
        let topDash: any = [];
        const dash = { ...selectedDash?.json, dashboard_id: selectedDash?.json?.id };
        let selectedDashbordExists = false;
        let defaultDashboard: any = {};
        let selectedDashbordPosition: any = -1;
        nlist.forEach((element: any, index: any) => {
          if (parseInt(dash?.dashboard_id) === element.dashboard_id) {
            selectedDashbordExists = true;
            selectedDashbordPosition = index;
          }
          if (topDash.length < 3) {
            topDash.push(element);
          }
          if (element.is_default) {
            defaultDashboard = element;
          }
        });
        if (!selectedDashbordExists && nlist.length !== 0) {
          const id = defaultDashboard?.dashboard_id ? defaultDashboard?.dashboard_id : nlist?.[0]?.dashboard_id;
          history.push({
            pathname: `${getDashboardsPage(projectParams)}/${id}`,
            search: location.search
          });
        } else {
          if (selectedDashbordPosition !== -1 && selectedDashbordPosition <= 2) {
            topDash.splice(selectedDashbordPosition, 1, dash);
          } else {
            topDash.push(dash);
          }
          setTopDashboards(topDash);
          setLoading(false);
          setList(nlist);
          setSearching(false);
        }
      }
    }
  }, [dashboardListState, selectedDash]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (dashboardDeleteState && actionDashId) {
      const { loading, error } = get(dashboardDeleteState, [actionDashId], { loading: true, error: true });
      if (!loading && !error) {
        const id = (actionDashId || "") as any;
        if (parseInt(id) === parseInt(selectedDash?.id)) {
          const dashboard: any = list.find((dash: any) => parseInt(dash?.dashboard_id) !== parseInt(selectedDash?.id));
          history.push({
            pathname: `${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.LIST}/${dashboard?.dashboard_id}`,
            search: location.search
          });
        } else {
          dashboardListSearch("");
        }
        setActionDashId(undefined);
      }
    }
  }, [dashboardDeleteState]); // eslint-disable-line react-hooks/exhaustive-deps

  const removeAllWidgetsData = () => {
    const widgets = Object.values(selectedDashboardWidgetsState || {});
    if (widgets.length) {
      widgets?.forEach((widget: any) => {
        const uri = get(widgetConstants, [widget.type, "uri"], "");
        dispatch(restapiClear(uri, "list", "-1"));
      });
    }
  };

  const setDefault = (dashboard: Record<string, any>) => {
    dispatch(ouDashboardSetDefault(params?.OU, dashboard?.dashboard_id));
    setDefaultLoading(dashboard?.dashboard_id);
  };

  const deleteDashboard = (dashboard_id: any) => {
    setActionDashId(dashboard_id);
    dispatch(dashboardsDelete(dashboard_id));
  };
  const memoizedDashListStyle = useMemo(() => ({ minHeight: "40vh", maxHeight: "40vh", overflowY: "auto" }), []);

  const dashList = useMemo(() => {
    return (
      // @ts-ignore
      <div style={memoizedDashListStyle}>
        {list.map((item: any, index: number) => (
          <AntRow key={`${index}-${item.name}`} className="flex">
            <AntCol
              span={`${item.dashboard_id === orgUnitGetState?.data?.default_dashboard_id ? 16 : 21}`}
              onClick={() => {
                loadDashboard(item.dashboard_id);
              }}>
              <AntTooltip
                className="dashboard-name"
                title={item?.name.length > DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH ? item?.name : null}>
                {item?.name.substring(0, DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH)}
                {item?.name.length > DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH ? "..." : ""}
              </AntTooltip>
            </AntCol>
            {item.dashboard_id === orgUnitGetState?.data?.default_dashboard_id && (
              <AntCol span={5} className="default-button">
                <AntButton size="small" type="default">
                  {" "}
                  Default{" "}
                </AntButton>
              </AntCol>
            )}
            <AntCol span={3}>
              <DashboardSearchPopoverActions
                isDefaultDisabled={item.dashboard_id === orgUnitGetState?.data?.default_dashboard_id}
                deleteDashboard={deleteDashboard}
                setDefault={setDefault}
                showClone={(id: any) => {
                  setShowCloneModal(true);
                  setActionDashId(id);
                }}
                dashboard={item}
              />
            </AntCol>
          </AntRow>
        ))}
        {!list.length && <Empty style={{ marginTop: "2vh" }} />}
      </div>
    );
  }, [list, orgUnitGetState?.data?.default_dashboard_id, location.search]);

  const memoizedStyle = useMemo(() => ({ width: "20rem" }), []);
  const memoizedBtnStyle = useMemo(() => ({ width: "100%", marginTop: "1rem", borderRadius: 0 }), []);

  const debouncedSearch = useCallback(
    debounce((v: string) => {
      dashboardListSearch(v);
      setSearchValue(v);
    }, 250),
    [props.ouId]
  );

  const handleSearchChange = useCallback(
    (v: string) => {
      debouncedSearch(v);
    },
    [props.ouId]
  );

  const manageDashboards = useCallback(() => {
    history.push({
      pathname: `${getDashboardsPage(projectParams)}/list`
    });
  }, []);
  const popupContent = useMemo(() => {
    if (loading || defaultLoadingDashboard || cloneData !== undefined) {
      return <Loader />;
    }
    return (
      <div className="more-dashboard" style={memoizedStyle}>
        <SearchInput onChange={handleSearchChange} loading={searching} />
        {dashList}
        {(!getIsStandaloneApp() || userRole?.toLowerCase() === USERROLES.ADMIN) && (
          <ButtonGroup className="dashboard-popup-btn-grp flex">
            <Tooltip style={memoizedBtnStyle} title={!entDashboard ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
              <Button
                className={`${!entDashboard ? "manage-dashboard-disabled" : "manage-dashboard"}`}
                disabled={!entDashboard}
                style={!entDashboard ? { width: "100%" } : memoizedBtnStyle}
                type="primary"
                onClick={manageDashboards}>
                Manage Insights
              </Button>
            </Tooltip>
          </ButtonGroup>
        )}
      </div>
    );
  }, [searching, dashList, entDashboard, orgUnitUpdateStates, defaultLoadingDashboard]);

  const handleVisibleChange = useCallback(visible => setShowPopover(visible), []);

  const ddButton = useMemo(
    () => (
      <span>
        {list && (
          <Button type={"ghost"} className="dashboard-title-button flex align-center" style={{ paddingLeft: 0 }}>
            {location.search && <span className="seperator">|</span>}{" "}
            <span className="more-dashboard">All Insights</span>
            <AntIconComponent className={`${showPopover ? "caret-down flex-item" : "flex-item"}`} type="down" />
          </Button>
        )}
      </span>
    ),
    [topDashboards, list, showPopover]
  );

  const loadDashboard = (id: string) => {
    if (selectedDash?.id === id) {
      return;
    }
    removeAllWidgetsData();
    history.push({
      pathname: `${getDashboardsPage(projectParams)}/${id}`,
      search: location.search
    });
  };

  const topLevelDashboards = useMemo(() => {
    return (
      <Breadcrumb separator={"|"} className="mb-5">
        {(topDashboards || [])?.map((dash: any, index: number) => {
          return (
            <Breadcrumb.Item
              onClick={() => {
                loadDashboard(dash.dashboard_id);
              }}
              key={`${dash?.name}-${index}`}>
              <span className={`link ${selectedDash?.id === dash?.dashboard_id ? "selected" : ""}`}>
                <AntTooltip title={dash?.name.length > DASHBOARD_ELIPSIS_LENGTH ? dash?.name : null}>
                  {dash?.name.substring(0, DASHBOARD_ELIPSIS_LENGTH)}
                  {dash?.name.length > DASHBOARD_ELIPSIS_LENGTH ? "..." : ""}
                </AntTooltip>
              </span>
            </Breadcrumb.Item>
          );
        })}
      </Breadcrumb>
    );
  }, [topDashboards, selectedDash?.id, location.search]);

  const debouncedCloneNameSearch = useCallback(
    debounce((value: string) => {
      dispatch(
        dashboardsList(
          {
            filter: {
              partial: { name: value },
              workspace_id:
                !!params?.workspace_id || !!selectedWorkspace?.id
                  ? parseInt(params?.workspace_id ?? selectedWorkspace?.id)
                  : ""
            }
          },
          SEARCH_DASH
        )
      );
      setNameSearching(true);
    }, 250),
    [selectedWorkspace]
  );

  const handleCloneNameSeach = useCallback((value: string) => {
    setSearchName(value);
    debouncedCloneNameSearch(value);
  }, []);

  useEffect(() => {
    if (actionDashId) {
      const { loading: _loading, error } = get(createState, ["0"], { loading: true, error: true });
      if (!_loading && !error) {
        const data = get(createState, ["0", "data"], {});
        setActionDashId(undefined);
        const rootOU: any = cloneDashPivotList?.find((elm: any) => elm.id === cloneData?.categories[0]);
        setCloneData(undefined);
        if (rootOU) {
          history.push(
            WebRoutes.dashboard.dashboard_ou.root(projectParams, data?.id, rootOU?.root_ou_ref_id, rootOU?.root_ou_id)
          );
        } else {
          history.push(`${getHomePage(projectParams)}`);
        }
      }
    }
  }, [createState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (dashboardDetailState && actionDashId) {
      const { loading: _loading, error } = get(dashboardDetailState, [actionDashId], { loading: true, error: true });
      if (!_loading && !error) {
        const data = get(dashboardDetailState, [actionDashId, "data"], {});
        if (cloneData?.name) {
          const _widgets = Object.values(widgets);
          const nWidgets = _widgets
            .map(w => new RestWidget(w))
            .filter(widget => {
              return (
                parseInt(widget.dashboard_id) === actionDashId && !widget.deleted && RestWidget.isValidWidget(widget)
              );
            })
            .map(w => w.json);
          let updatedWidgets = [];

          if (nWidgets && nWidgets.length > 0) {
            const filteredWidgets = filteredDeprecatedWidgets(nWidgets as any);
            updatedWidgets = cloneWidgets(filteredWidgets);
          }
          const newDashboard = {
            ...data,
            widgets: updatedWidgets,
            name: cloneData?.name,
            default: false,
            category: cloneData.categories
          };
          dispatch(dashboardsCreate(newDashboard));
        }
      }
    }
  }, [dashboardDetailState, widgets, cloneData]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      {topLevelDashboards}
      <div style={{ display: "flex", alignItems: "center" }}>
        <Popover
          className={"dash-search-popover"}
          placement={"bottomLeft"}
          content={popupContent}
          trigger="click"
          visible={showPopover && !showCloneModal}
          onVisibleChange={handleVisibleChange}
          align={{
            overflow: { adjustX: false, adjustY: false }
          }}
          // @ts-ignore
        >
          {ddButton}
        </Popover>
        {showCloneModal && (
          <EditCloneModal
            visible={showCloneModal}
            title={"Clone Insight"}
            onOk={(name: string, categories: any) => {
              setCloneData({ name, categories });
              setShowCloneModal(false);
              dispatch(dashboardsGet(actionDashId));
              setShowPopover(false);
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
    </>
  );
};
