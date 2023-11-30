import { Card, Spin } from "antd";
import Search from "antd/lib/input/Search";
import Loader from "components/Loader/Loader";
import { orgUnitJSONType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { OU_DASHBOARD_LIST_ID } from "configurations/pages/Organization/Constants";
import {
  ORG_CATEGORY_DASHBOARD_REVERSE,
  ORG_UNIT_TREE_VIEW_ID
} from "configurations/pages/Organization/organization-unit/components/org-unit-tree-view/constant";
import OrgUnitTreeViewComponent from "configurations/pages/Organization/organization-unit/components/org-unit-tree-view/OrgUnitTreeViewComponent";
import { getDashboardsPage, Organization_Routes, getBaseUrl } from "constants/routePaths";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get, unionBy } from "lodash";
import queryString from "query-string";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { dashboardsList, genericList, restapiClear, setSelectedEntity } from "reduxConfigs/actions/restapi";
import { orgUnitDashboardList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { widgetsSelector } from "reduxConfigs/selectors/widgetSelector";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { WebRoutes } from "routes/WebRoutes";
import { AntRow, AntCol, AntButton, AntIcon, AntTooltip, AntText } from "shared-resources/components";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { nestedSort, shiftArrayByKey } from "utils/arrayUtils";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { NO_DASHBOARD_IN_WORKSPACE_MSG, NO_DASHBOARD_MSG, NO_OU_MSG, NO_SEARCH_DASHBOARD_MSG } from "./constant";
import { OrganizationTabKey } from "configurations/pages/Organization/Constants";
import { buildLink } from "./helper";
import "./LandingPage.scss";
import { ProjectPathProps } from "classes/routeInterface";
import { useParentProvider } from "contexts/ParentProvider";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { ButtonVariation } from "@harness/uicore";
import { useAppStore } from "contexts/AppStoreContext";
import { getIsStandaloneApp } from "helper/helper";

interface LandingTreePageProps {
  selectedPivot: Record<any, any>;
}

const LandingTreePage: React.FC<LandingTreePageProps> = props => {
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const params = queryString.parse(location.search) as any;
  const [dashboards, setDashboards] = useState<Array<OUDashboardType>>([]);
  const [selectedDasboard, setSelectedDashboard] = useState<number>();
  const [dashboardsLoading, setDashboardLoading] = useState<boolean>(false);
  const [selectedOu, setSelectedOu] = useState<string>("");
  const [rootOUName, setRootOUName] = useState<string>("");
  const [ouMappings, setOuMappings] = useState<Record<string, any>>({});
  const [selectedNodes, setSelectedNodes] = useState<string[]>([]);
  const [searchValue, setSearch] = useState<string>("");
  const [workspaceHasDashboards, setWorkspaceHasDashboards] = useState<boolean>(true);
  const [hasAssociatedDashboards, setHasAssociatedDashboards] = useState<boolean>(true);
  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const getSelectedWorkspaceState = useSelector(getSelectedWorkspace);
  const projectParams = useParams<ProjectPathProps>();

  const {
    components: { RbacButton }
  } = useParentProvider();
  const { accountInfo } = useAppStore();

  const ouDahboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_dashboard_list",
    method: "list",
    uuid: OU_DASHBOARD_LIST_ID
  });

  const workspaceDashboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "dashboards",
    method: "list",
    uuid: OU_DASHBOARD_LIST_ID
  });

  useEffect(() => {
    const loading = get(workspaceDashboardListState, ["loading"], true);
    const error = get(workspaceDashboardListState, ["error"], true);
    if (!loading && !error) {
      const records = get(workspaceDashboardListState, ["data", "records"], []);
      setWorkspaceHasDashboards(!!records.length);
    }
  }, [workspaceDashboardListState]);

  const orgUnitListState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: ORG_UNIT_TREE_VIEW_ID
  });

  const orgCategoryDashboardReverseState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: ORG_CATEGORY_DASHBOARD_REVERSE
  });
  const selectedDashboardWidgetsState = useSelector(widgetsSelector);
  useEffect(() => {
    if (params?.dashboard_id) {
      dispatch(
        genericList(
          "organization_unit_management",
          "list",
          {
            filter: {
              ou_category_id: [props?.selectedPivot.id],
              dashboard_id: params?.dashboard_id
            }
          },
          null,
          ORG_CATEGORY_DASHBOARD_REVERSE
        )
      );
    }
    dispatch(
      dashboardsList({ filter: { workspace_id: parseInt(getSelectedWorkspaceState.id || "") } }, OU_DASHBOARD_LIST_ID)
    );
    return () => {
      dispatch(restapiClear("organization_unit_management", "list", OU_DASHBOARD_LIST_ID));
      dispatch(restapiClear("organization_unit_management", "list", ORG_CATEGORY_DASHBOARD_REVERSE));
      dispatch(restapiClear("organization_unit_management", "list", ORG_UNIT_TREE_VIEW_ID));
    };
  }, []);

  useEffect(() => {
    if (dashboardsLoading) {
      const loading = get(ouDahboardListState, ["loading"], true);
      const error = get(ouDahboardListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records: Array<OUDashboardType> = get(ouDahboardListState, ["data", "records"], []);
          const dashboards: OUDashboardType[] =
            shiftArrayByKey(nestedSort(records, "ou_id", "dashboard_order"), "ou_id", ouMappings[selectedOu].ou_id) ||
            [];
          const uniqueDashboards = unionBy(dashboards, "dashboard_id");
          setHasAssociatedDashboards(!!uniqueDashboards.find(d => d.ou_id === ouMappings[selectedOu]?.ou_id));
          setDashboards(uniqueDashboards);
        }
        setDashboardLoading(false);
      }
    }
  }, [ouDahboardListState, dashboardsLoading, selectedOu]);

  const loadDashboards = (key: string, redirect: string | number | undefined = undefined) => {
    const ou = ouMappings?.[key]?.id || "";
    setSelectedDashboard(ouMappings?.[ou]?.default_dashboard_id);
    setDashboardLoading(true);
    setSelectedOu(ou);
    dispatch(
      orgUnitDashboardList(
        OU_DASHBOARD_LIST_ID,
        sanitizeObjectCompletely({
          ou_id: ouMappings?.[key]?.ou_id,
          inherited: ORG_UNIT_TREE_VIEW_ID === ouMappings?.[key]?.parent_ref_id ? false : true
        })
      )
    );
    if (redirect) {
      redirectToDashboard(params?.dashboard_id, ou);
    }
  };

  const treeData = useMemo(() => {
    const nOuIdMapping: any = {};
    let root: any = null;
    if (orgUnitListState?.data?.records?.length > 0 && Object.keys(ouMappings).length === 0) {
      orgUnitListState?.data?.records?.filter((record: orgUnitJSONType) => {
        nOuIdMapping[record?.id || ""] = {
          ...record
        };
        if (!record.parent_ref_id) {
          root = record;
        }
      });
      setOuMappings(nOuIdMapping);
      if (root) {
        setRootOUName(root?.name);
        setSelectedNodes([root?.id]);
      }
    }
    return (
      <OrgUnitTreeViewComponent
        ouGroupId={props?.selectedPivot.id}
        title={null}
        extra={
          !orgUnitListState?.loading && orgUnitListState?.data?.records?.length === 0 ? (
            <span>
              <AntIcon type="info-circle" />
              <span className="pl-9">{NO_OU_MSG}</span>
            </span>
          ) : (
            <></>
          )
        }
        loadDashboards={loadDashboards}
        dashboard_id={parseInt(params?.dashboard_id)}
        setSelectedNodes={setSelectedNodes}
        selectedNodes={selectedNodes.length === 0 ? [root?.id] : selectedNodes}
      />
    );
  }, [props.selectedPivot?.id, orgUnitListState, loadDashboards]);

  const removeAllWidgetsData = () => {
    const widgets = Object.values(selectedDashboardWidgetsState || {});
    if (widgets.length) {
      widgets?.forEach((widget: any) => {
        const uri = get(widgetConstants, [widget.type, "uri"], "");
        dispatch(restapiClear(uri, "list", "-1"));
      });
    }
  };

  const redirectToDashboard = (id: any, OU: any) => {
    dispatch(restapiClear("dashboards", "get", id));
    dispatch(setSelectedEntity("custom-data-loading", { loading: true }));
    removeAllWidgetsData();
    const obj = buildLink(ouMappings, OU, []);
    const search = `?OU=${OU}&workspace_id=${getSelectedWorkspaceState?.id}`;
    history.push({
      pathname: `${getDashboardsPage(projectParams)}/${id}`,
      search
    });
  };

  const redirectToOrg = () => {
    history.push(
      `${getBaseUrl(projectParams)}${Organization_Routes._ROOT}?ou_workspace_id=${getSelectedWorkspaceState?.id}`
    );
  };
  const redirectToDashboards = () => {
    history.push(`${getBaseUrl(projectParams)}/dashboards/list`);
  };
  const handleAddButtonClicked = () => {
    history.push(WebRoutes.dashboard.create(projectParams, props?.selectedPivot?.id));
  };

  const handleAssociateButtonClicked = () => {
    history.push(
      WebRoutes.organization_page.edit(
        projectParams,
        selectedOu,
        getSelectedWorkspaceState?.id,
        props.selectedPivot?.id,
        OrganizationTabKey.OU_DASHBOARDS
      )
    );
  };

  const onDashboardSearch = (e: any) => {
    const { value } = e.target;
    setSearch(value);
    let records: Array<OUDashboardType> = get(ouDahboardListState, ["data", "records"], []);
    if (value) {
      records = records.filter((item: any) => {
        if (item.name.toLocaleLowerCase().includes(value.toLocaleLowerCase())) {
          return item;
        }
      });
      setDashboards(records);
    } else {
      records =
        shiftArrayByKey(nestedSort(records, "ou_id", "dashboard_order"), "ou_id", ouMappings[selectedOu].ou_id) || [];
      setDashboards(unionBy(records, "dashboard_id"));
    }
  };

  const renderAddAssociateButtonComponent = useMemo(() => {
    if (!searchValue && (!workspaceHasDashboards || (!dashboardsLoading && !hasAssociatedDashboards))) {
      let buttonMSG = "";
      let buttonText = "";
      let buttonClickHandler = undefined;
      if (!workspaceHasDashboards) {
        buttonMSG = NO_DASHBOARD_IN_WORKSPACE_MSG;
        buttonText = "Add a new dashboard to this Collection";
        buttonClickHandler = handleAddButtonClicked;
      } else if (!dashboardsLoading && !hasAssociatedDashboards) {
        buttonMSG = NO_DASHBOARD_MSG;
        buttonText = "Associate insights to this Collection";
        buttonClickHandler = handleAssociateButtonClicked;
      }

      return (
        <div className="flex flex-column">
          <AntText className="mb-10">{buttonMSG}</AntText>
          {getIsStandaloneApp() ? (
            <AntButton className={"add-dashboard"} type="primary" onClick={buttonClickHandler}>
              {buttonText}
            </AntButton>
          ) : (
            <RbacButton
              variation={ButtonVariation.PRIMARY}
              className="add-dashboard"
              text={buttonText}
              onClick={buttonClickHandler}
              permission={{
                permission: PermissionIdentifier.EDIT_SEI_COLLECTIONS,
                resource: {
                  resourceType: ResourceType.SEI_COLLECTIONS,
                  resourceIdentifier: selectedOu
                },
                resourceScope: {
                  accountIdentifier: accountInfo?.identifier || ""
                }
              }}
            />
          )}
        </div>
      );
    }
    return <></>;
  }, [searchValue, workspaceHasDashboards, dashboardsLoading, hasAssociatedDashboards, selectedOu]);

  return (
    <div className="no-scroll">
      <div className="landing-page-body">
        <AntRow className="header-row flex">
          <AntCol className="flex title" span={11}>
            <span className="group-label flex">Select a collection</span>
            <AntTooltip title="Manage Collections">
              <AntIcon onClick={redirectToOrg} type="setting" />
            </AntTooltip>
          </AntCol>
          {params?.dashboard_id === undefined && (
            <>
              <AntCol className="divider" />
              <AntCol />
              <AntCol className="m-l-5 flex title" span={11}>
                <span className="group-label flex">Select an Insight</span>
                <AntTooltip title="Manage Insights">
                  <AntIcon onClick={!selectedWorkspace?.demo ? redirectToDashboards : () => {}} type="setting" />
                </AntTooltip>
              </AntCol>
            </>
          )}
        </AntRow>
        <AntRow className="nav-group flex">
          <AntCol span={11} className="flex flex-column">
            {!orgUnitListState?.loading && orgUnitListState?.data?.records?.length === 0 && (
              <span className="no-ou-msg">
                <AntIcon type="info-circle" />
                <span className="pl-9">{NO_OU_MSG}</span>
              </span>
            )}
            {!params?.dashboard_id ? (
              treeData
            ) : orgCategoryDashboardReverseState?.loading || orgCategoryDashboardReverseState?.error ? (
              <Loader />
            ) : (
              treeData
            )}
          </AntCol>
          {params?.dashboard_id === undefined && <AntCol className="divider" />}
          {selectedOu && params?.dashboard_id === undefined && (
            <AntCol className="dashboard-col" span={11}>
              <Card extra={<></>} title={null} bordered={false}>
                <Search
                  className="dashboard-search-input"
                  placeholder="Search"
                  allowClear
                  onChange={onDashboardSearch}
                />
                {dashboardsLoading && (
                  <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
                    <Spin />
                  </div>
                )}
                {!selectedWorkspace?.demo &&
                  ouDahboardListState?.data?.records?.length === 0 &&
                  renderAddAssociateButtonComponent}
                <div className="list-wrapper">
                  {!dashboardsLoading &&
                    dashboards.map((row, index: number) => {
                      const isSelected = (selectedDasboard ?? "").toString() === (row?.dashboard_id ?? "").toString();
                      const dashboardName: string = get(row, ["display_name"], row.name);
                      const searchIndex = dashboardName.toLowerCase().indexOf(searchValue.toLowerCase());
                      const beforeStr = dashboardName.substr(0, searchIndex);
                      const found = dashboardName.substr(searchIndex, searchValue.length);
                      const afterStr = dashboardName.substr(searchIndex + searchValue.length);
                      const name =
                        searchIndex > -1 ? (
                          <span>
                            {beforeStr}
                            <span style={{ color: "#f50" }}>{found}</span>
                            {afterStr}
                          </span>
                        ) : (
                          <span>{dashboardName}</span>
                        );
                      return (
                        <AntRow
                          key={`${row.name}-${index}`}
                          className={`dashboard-list flex ${isSelected ? "selected" : null}`}
                          onClick={(e: any) => {
                            e.preventDefault();
                            redirectToDashboard(row.dashboard_id, selectedOu);
                          }}>
                          <span className="dashboard-name">{name}</span>
                          {row.dashboard_id === ouMappings?.[selectedOu]?.default_dashboard_id && (
                            <span className="default-button">
                              <AntButton size="small" type="default">
                                {" "}
                                Default{" "}
                              </AntButton>
                            </span>
                          )}
                          <span className="select-icon">
                            <SvgIconComponent icon="arrowRightCircle" />
                          </span>
                        </AntRow>
                      );
                    })}
                  {searchValue && dashboards.length === 0 && NO_SEARCH_DASHBOARD_MSG}
                </div>
              </Card>
            </AntCol>
          )}
        </AntRow>
      </div>
    </div>
  );
};

export default React.memo(LandingTreePage);
