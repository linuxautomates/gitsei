import { Breadcrumb, Button, Popover } from "antd";
import { RestDashboard } from "classes/RestDashboards";
import { OUDashboardType } from "configurations/configuration-types/OUTypes";
import { cloneDeep, get } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import queryString from "query-string";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { orgUnitDashboardList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { shiftArrayByKey, nestedSort } from "utils/arrayUtils";
import { OU_DEMO_DASHBOARD_SEARCH_LIST_ID } from "./constant";
import { AntCol, AntRow, AntTooltip } from "shared-resources/components";
import { restapiClear } from "reduxConfigs/actions/restapi";
import "./demo-dashboard-header-search.styles.scss";
import {
  DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH,
  DASHBOARD_ELIPSIS_LENGTH
} from "configurations/pages/Organization/Constants";
import Loader from "components/Loader/Loader";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import AntIconComponent from "shared-resources/components/ant-icon/ant-icon.component";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";

interface DemoDashboardHeaderSearchProps {
  ouId?: string;
}
const DemoDashboardHeaderSearchComponent: React.FC<DemoDashboardHeaderSearchProps> = ({ ouId }) => {
  const [ouDashboardLoading, setOUDashboardLoading] = useState<boolean>(false);
  const [ouDashboards, setOUDashboards] = useState<OUDashboardType[]>([]);
  const [topDashboards, setTopDashboards] = useState<OUDashboardType[]>([]);
  const [searchValue, setSearchValue] = useState<string>("");
  const [showPopover, setShowPopover] = useState<boolean>(false);

  const history = useHistory();
  const location = useLocation();
  const dispatch = useDispatch();
  const projectParams = useParams<ProjectPathProps>();

  const dashboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_dashboard_list",
    method: "list",
    uuid: OU_DEMO_DASHBOARD_SEARCH_LIST_ID
  });

  const selectedDash: RestDashboard = useSelector(selectedDashboard) as RestDashboard;
  const { ou_category_id: ou_ids } = queryString.parse(location.search);

  useEffect(() => {
    dispatch(
      orgUnitDashboardList(OU_DEMO_DASHBOARD_SEARCH_LIST_ID, {
        ou_id: ouId,
        inherited: true
      })
    );
    setOUDashboardLoading(true);
    return () => {
      dispatch(restapiClear("org_dashboard_list", "list", OU_DEMO_DASHBOARD_SEARCH_LIST_ID));
    };
  }, []);

  useEffect(() => {
    if (ouDashboardLoading) {
      const loading = get(dashboardListState, ["loading"], true);
      const error = get(dashboardListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const responseDashboards: OUDashboardType[] = get(dashboardListState, ["data", "records"], []);
          if (responseDashboards.length) {
            const ouIds: Array<string> = ((ou_ids ?? "") as string)?.split(",");
            const orderedDashboards: OUDashboardType[] =
              shiftArrayByKey(
                nestedSort(responseDashboards, "ou_id", "dashboard_order"),
                "ou_id",
                ouIds?.[ouIds.length - 1] || undefined
              ) || [];
            const topDash: OUDashboardType[] = [];
            orderedDashboards.forEach((d: OUDashboardType) => {
              if (topDash.length < 3) {
                topDash.push(d);
              }
            });
            setTopDashboards(topDash);
            setOUDashboards(orderedDashboards);
          }
        }
        setOUDashboardLoading(false);
      }
    }
  }, [dashboardListState, ouDashboardLoading, ou_ids, selectedDash]);

  const loadDashboard = useCallback(
    (id: string) => {
      if (selectedDash?.id?.toString() === id?.toString()) {
        return;
      }
      history.push({
        pathname: `${getDashboardsPage(projectParams)}/${id}`,
        search: location.search
      });
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [selectedDash, location.search]
  );

  const topLevelDashboards = useMemo(() => {
    let selectedDashbordExists = false;
    let selectedDashbordPosition: number = -1;
    const nTopDashboards = cloneDeep(topDashboards);
    const selectedDashboard: OUDashboardType | undefined = ouDashboards?.find(
      d => (d.dashboard_id ?? "").toString() === selectedDash?.json?.id?.toString()
    );
    const dash = {
      dashboard_id: selectedDashboard?.dashboard_id,
      name: selectedDashboard?.display_name ?? selectedDashboard?.name
    };
    ouDashboards.forEach((d: OUDashboardType, index: number) => {
      if (dash?.dashboard_id?.toString() === d?.dashboard_id?.toString()) {
        selectedDashbordExists = true;
        selectedDashbordPosition = index;
      }
    });
    if (selectedDashbordExists) {
      if (selectedDashbordPosition !== -1 && selectedDashbordPosition <= 2) {
        nTopDashboards.splice(selectedDashbordPosition, 1, dash as OUDashboardType);
      } else {
        nTopDashboards.push(dash as OUDashboardType);
      }
    }

    return (
      <Breadcrumb separator={"|"} className="mb-5">
        {(nTopDashboards || [])?.map((dash: OUDashboardType, index: number) => {
          const name = dash?.display_name ?? dash.name;
          return (
            <Breadcrumb.Item
              onClick={() => {
                loadDashboard(dash.dashboard_id);
              }}
              key={`${dash?.name}-${index}`}>
              <span
                className={`link ${selectedDash?.id?.toString() === dash?.dashboard_id?.toString() ? "selected" : ""}`}>
                <AntTooltip title={name.length > DASHBOARD_ELIPSIS_LENGTH ? name : null}>
                  {name.substring(0, DASHBOARD_ELIPSIS_LENGTH)}
                  {name.length > DASHBOARD_ELIPSIS_LENGTH ? "..." : ""}
                </AntTooltip>
              </span>
            </Breadcrumb.Item>
          );
        })}
      </Breadcrumb>
    );
  }, [topDashboards, selectedDash, loadDashboard, ouDashboards]);

  const handleSearchChange = useCallback((v: string) => {
    setSearchValue(v);
  }, []);

  const popupContent = useMemo(() => {
    if (ouDashboardLoading) {
      return <Loader />;
    }
    const filteredDashboards = !!searchValue
      ? ouDashboards.filter(d => (d?.display_name ?? d?.name)?.toLowerCase().includes(searchValue?.toLowerCase()))
      : ouDashboards;

    return (
      <div className="more-demo-dashboard">
        <SearchInput onChange={handleSearchChange} />
        <div className="more-demo-dashboard_scroll-container">
          {filteredDashboards.map((item: OUDashboardType, index: number) => (
            <AntRow key={`${index}-${item.name}`} className="flex">
              <AntCol
                onClick={() => {
                  loadDashboard(item.dashboard_id);
                }}>
                <AntTooltip
                  className="dashboard-name"
                  title={
                    (item?.display_name ?? item?.name)?.length > DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH
                      ? item?.display_name ?? item?.name
                      : null
                  }>
                  {(item?.display_name ?? item?.name).substring(0, DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH)}
                  {(item?.display_name ?? item?.name).length > DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH ? "..." : ""}
                </AntTooltip>
              </AntCol>
            </AntRow>
          ))}
        </div>
      </div>
    );
  }, [ouDashboardLoading, ouDashboards, handleSearchChange, loadDashboard, searchValue]);

  const ddButton = useMemo(
    () => (
      <span>
        {ouDashboards && (
          <Button type={"ghost"} className="demo-dashboard-title-button flex align-center" style={{ paddingLeft: 0 }}>
            {location.search && <span className="seperator">|</span>}{" "}
            <span className="more-dashboard">All Dashboards</span>
            <AntIconComponent className={`${showPopover ? "caret-down flex-item" : "flex-item"}`} type="down" />
          </Button>
        )}
      </span>
    ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [ouDashboards, showPopover]
  );

  const handleVisibleChange = useCallback(visible => setShowPopover(visible), []);

  return (
    <>
      {topLevelDashboards}
      <div style={{ display: "flex", alignItems: "center" }}>
        <Popover
          className={"dash-search-popover"}
          placement={"bottomLeft"}
          content={popupContent}
          trigger="click"
          visible={showPopover}
          onVisibleChange={handleVisibleChange}
          align={{
            overflow: { adjustX: false, adjustY: false }
          }}>
          {ddButton}
        </Popover>
      </div>
    </>
  );
};

export default DemoDashboardHeaderSearchComponent;
