import React, { useCallback, useEffect, useState } from "react";
import { Breadcrumb, Icon } from "antd";
import "./DashboardOUHeader.scss";
import queryString from "query-string";
import { AntText, AntTooltip, AntButton } from "shared-resources/components";
import DashboardOUHeaderGroupList from "./DashboardOUGroupList";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { HeaderPivot, PivotType } from "configurations/configuration-types/OUTypes";
import { useDispatch } from "react-redux";
import { orgUnitDashboardList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import {
  DASHBOARD_ELIPSIS_LENGTH,
  ORGANIZATION_UNIT_NODE,
  OU_DASHBOARD_LIST_ID
} from "configurations/pages/Organization/Constants";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import Loader from "components/Loader/Loader";
import { orderedOUList } from "./helper";
import { getBaseUrl, Organization_Routes } from "constants/routePaths";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import WorkspaceSelectDropdownComponent from "core/containers/header/select-dropdowns/workspace-select-dropdown/WorkspaceSelectDropdownComponent";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { getElipsis, getSubString } from "utils/stringUtils";
import { ProjectPathProps } from "classes/routeInterface";

interface DashboardOUHeaderPorps {
  dashboardId?: string;
  demoDashboard?: boolean;
}
export const DashboardOUHeader: React.FC<DashboardOUHeaderPorps> = ({ demoDashboard }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const [showPopover, setShowPopover] = useState(false);
  const params = queryString.parse(location.search) as any;
  const [breadcrumbs, setBreadcrumbs] = useState<Array<HeaderPivot>>([]);
  const [ouLoading, setOULoading] = useState<boolean>(false);
  const UUID = "ORG_TREE_LIST";
  const projectParams = useParams<ProjectPathProps>();
  const { workSpaceListData, loading } = useWorkSpaceList();
  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: ORGANIZATION_UNIT_NODE,
    method: "list",
    uuid: UUID
  });
  useEffect(() => {
    if (ouLoading) {
      const loading = get(pivotsListState, ["loading"], true);
      const error = get(pivotsListState, ["error"], true);
      if (!loading) {
        if (!error) {
          let records: any = get(pivotsListState, ["data", "records"], []);
          const root = records.find((record: HeaderPivot) => !record.parent_ref_id);
          if (root) {
            records = orderedOUList(root, records, [root]);
          }
          setBreadcrumbs(records);
        }
        setOULoading(false);
      }
    }
  }, [pivotsListState]);

  useEffect(() => {
    let ouGroupId = params?.ou_category_id?.split(",") || [];
    setOULoading(true);
    dispatch(
      genericList(
        ORGANIZATION_UNIT_NODE,
        "list",
        {
          filter: { ou_id: [...ouGroupId] }
        },
        null,
        UUID
      )
    );
    dispatch(setPageSettings(location.pathname, { search: location.search }));
  }, []);

  const orgTreeState = (records: Array<HeaderPivot>, OU: string) => {
    const ou_id = records.map(rec => {
      return rec.ou_id;
    });
    let newParams: any = { OU, ou_category_id: ou_id?.join(",") };
    let stringified: string = queryString.stringify(newParams);
    history.push({
      search: "?" + stringified
    });
  };
  const handlePivotClick = useCallback(
    (item: HeaderPivot, isRoot?: boolean) => {
      let nbreadcrumb = [...breadcrumbs];
      if (isRoot) {
        const index = breadcrumbs.findIndex(record => {
          return record.id === item.id;
        });
        nbreadcrumb = nbreadcrumb.slice(0, index + 1);
      } else {
        nbreadcrumb = [...breadcrumbs, item];
      }
      orgTreeState(nbreadcrumb, item.id);
      setBreadcrumbs(nbreadcrumb);
      setShowPopover(false);
      dispatch(
        orgUnitDashboardList(OU_DASHBOARD_LIST_ID, {
          ou_id: item.ou_id,
          inherited: nbreadcrumb.length > 1 ? true : false
        })
      );
    },
    [params]
  );

  const manageOU = useCallback(() => {
    history.push({
      pathname: `${getBaseUrl(projectParams)}${Organization_Routes._ROOT}`
    });
  }, []);

  return (
    <>
      {ouLoading && <Loader />}
      {!ouLoading && (
        <div className="ou-header">
          <Breadcrumb separator={"\\"} className="mb-5">
            {(breadcrumbs || []).map((item, index) => {
              if (index === breadcrumbs?.length - 1) {
                return (
                  <>
                    <DashboardOUHeaderGroupList
                      handlePivotClick={handlePivotClick}
                      key={`${item?.name}-${index}`}
                      showPopover={showPopover}
                      setShowPopover={setShowPopover}
                      ou_category_id={item.ou_category_id}
                      parent_ref_id={item.id}
                      manageOU={manageOU}
                    />
                    <span
                      className="ou-name"
                      key={`${item.name}`}
                      onClick={() => {
                        setShowPopover(state => !state);
                      }}>
                      <AntTooltip title={item?.name?.length > DASHBOARD_ELIPSIS_LENGTH ? item?.name : null}>
                        <AntText className="selected">
                          {" "}
                          {getSubString(item?.name, DASHBOARD_ELIPSIS_LENGTH)}
                          {getElipsis(item?.name, DASHBOARD_ELIPSIS_LENGTH)}
                        </AntText>
                      </AntTooltip>
                      <Icon type="down" />
                    </span>
                  </>
                );
              } else {
                return (
                  <Breadcrumb.Item
                    onClick={() => {
                      handlePivotClick(item, true);
                    }}
                    key={index}>
                    <span className="link">{item.name}</span>
                  </Breadcrumb.Item>
                );
              }
            })}
          </Breadcrumb>
          {window.isStandaloneApp && !demoDashboard && (
            <WorkspaceSelectDropdownComponent
              workspaces={workSpaceListData ?? []}
              loading={loading}
              className="dashboard-workspace-selection-container"
            />
          )}
        </div>
      )}
    </>
  );
};

export default React.memo(DashboardOUHeader);
