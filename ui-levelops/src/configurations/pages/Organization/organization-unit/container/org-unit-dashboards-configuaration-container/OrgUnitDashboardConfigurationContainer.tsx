import React, { useEffect, useMemo, useState } from "react";
import queryString from "query-string";
import { Spin, Typography } from "antd";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { orgUnitBasicInfoType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { get, isEqual, uniqBy } from "lodash";
import { _dashboardsListSelector } from "reduxConfigs/selectors/dashboardSelector";
import { AntButton, AntIcon, AntText } from "shared-resources/components";
import "./orgUnitDashboardConfigurationContainer.styles.scss";
import OrgUnitAvailableDashboardsContainer from "./org-unit-available-dashboard-container/OrgUnitAvailableDashboardsContainer";
import { baseColumnConfig } from "utils/base-table-config";
import OrgUnitAssociatedDashboardsContainer from "./org-unit-associated-dashboards-container/OrgUnitAssociatedDashboardsContainer";
import { useDispatch } from "react-redux";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { ORG_UNIT_DASHBOARDS_ASSOCIATION_ID } from "configurations/pages/Organization/Constants";
import { dashboardsList, restapiClear } from "reduxConfigs/actions/restapi";
import { OrganizationUnitDashboards } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { usePrevious } from "shared-resources/hooks/usePrevious";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { AVAILABLE_DASHBOARDS_ID } from "./constant";
import { setOUHeaderSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_OU_HEADER_WORKSPACE } from "reduxConfigs/selectors/workspace/constant";
import { ColumnProps } from "antd/lib/table";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";

interface OrgUnitDashboardConfigurationProps {
  orgUnit: RestOrganizationUnit;
  handleOUChanges: (key: orgUnitBasicInfoType, value: any) => void;
}
const OrgUnitDashboardConfigurationContainer: React.FC<OrgUnitDashboardConfigurationProps> = (
  props: OrgUnitDashboardConfigurationProps
) => {
  const { orgUnit, handleOUChanges } = props;
  const [selectedDashboards, setSelectedDashboards] = useState<Array<OUDashboardType>>(orgUnit?.dashboards);
  const [deSelectedDashboards, setDeSelectedDashboards] = useState<Array<string>>([]);
  const [inheritedDashboards, setInheritedDashboards] = useState<Array<OUDashboardType>>([]);
  const [inheritedDashboardListLoading, setInheritedDashboardListLoading] = useState<boolean>(false);
  const [dashboardListLoading, setDashboardListLoading] = useState<boolean>(true);
  const [showNoDashboardsView, setShowNoDashboardsView] = useState<boolean>(false);
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const prevParentId: number | undefined = usePrevious(orgUnit?.parentId);
  const associatedDashboardsState = useParamSelector(getGenericUUIDSelector, {
    uri: "ous_dashboards",
    method: "list",
    uuid: ORG_UNIT_DASHBOARDS_ASSOCIATION_ID
  });
  const { ou_workspace_id } = queryString.parse(location.search);
  const prevInheritedDashboards = usePrevious(inheritedDashboards);
  const projectParams = useParams<ProjectPathProps>();

  /** removing duplicate dashboards from the collection associated dashboard list */
  useEffect(() => {
    if (!isEqual(prevInheritedDashboards, inheritedDashboards)) {
      const inheritedDashboardIds: string[] = (inheritedDashboards ?? []).map(dash => dash.dashboard_id);
      const filteredOUAssociatedDashboards: OUDashboardType[] = (orgUnit?.dashboards ?? []).filter(
        dash => !inheritedDashboardIds.includes(dash.dashboard_id)
      );
      const filteredOUSelectedDashboards: OUDashboardType[] = (selectedDashboards ?? []).filter(
        dash => !inheritedDashboardIds.includes(dash.dashboard_id)
      );
      setSelectedDashboards(filteredOUSelectedDashboards);
      handleOUChanges("dashboards", filteredOUAssociatedDashboards);
    }
  }, [inheritedDashboards, prevInheritedDashboards, orgUnit?.dashboards, selectedDashboards, handleOUChanges]);

  const dashboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "dashboards",
    method: "list",
    uuid: AVAILABLE_DASHBOARDS_ID
  });

  useEffect(() => {
    if (orgUnit?.isParent) {
      dispatch(
        dashboardsList(
          {
            filter: {
              workspace_id: !!ou_workspace_id ? parseInt(ou_workspace_id as string) : ""
            }
          },
          AVAILABLE_DASHBOARDS_ID
        )
      );
    } else {
      setDashboardListLoading(false);
    }
  }, []);

  useEffect(() => {
    if (dashboardListLoading) {
      const loading = get(dashboardListState, ["loading"], true);
      const error = get(dashboardListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const totalCount: number = get(dashboardListState, ["data", "_metadata", "total_count"], 0);
          if (totalCount === 0) {
            setShowNoDashboardsView(true);
          }
          dispatch(restapiClear("dashboards", "list", AVAILABLE_DASHBOARDS_ID));
        }
        setDashboardListLoading(false);
      }
    }
  }, [dashboardListState, dashboardListLoading]);

  useEffect(() => {
    if (!isEqual(prevParentId, orgUnit.parentId) && !inheritedDashboardListLoading) {
      fetchDashboardList();
    }
  }, [orgUnit?.parentId, prevParentId, inheritedDashboardListLoading]);

  const fetchDashboardList = () => {
    if (orgUnit?.parentId) {
      dispatch(restapiClear("ous_dashboards", "list", ORG_UNIT_DASHBOARDS_ASSOCIATION_ID));
      setInheritedDashboardListLoading(true);
      dispatch(OrganizationUnitDashboards(orgUnit?.parentId?.toString()));
    }
  };

  useEffect(() => {
    if (inheritedDashboardListLoading) {
      if (Object.keys(associatedDashboardsState).length) {
        const inheritedDashboards: OUDashboardType[] = get(
          associatedDashboardsState,
          ["data", "records", "inherited_dashboards"],
          {}
        );
        setInheritedDashboards(inheritedDashboards);
        setInheritedDashboardListLoading(false);
      }
    }
  }, [associatedDashboardsState, inheritedDashboardListLoading]);

  const handleSelectedDashboards = (selectedDashboardKeys: Array<string>, rSelectedDashboards: Array<any>) => {
    let nSelecteddashboards: OUDashboardType[] = rSelectedDashboards.map(
      dash =>
        ({
          ou_id: orgUnit.ouId,
          is_default: false,
          dashboard_id: dash.id,
          name: dash.name,
          display_name: get(dash, ["metadata", "display_name"])
        } as OUDashboardType)
    );
    nSelecteddashboards = [...selectedDashboards, ...nSelecteddashboards].filter(dash =>
      selectedDashboardKeys.includes(dash.dashboard_id)
    );
    setSelectedDashboards(uniqBy(nSelecteddashboards, "dashboard_id"));
  };

  const handleDeSelectedDashboards = (deSelectedDashboardKeys: Array<string>) => {
    setDeSelectedDashboards(deSelectedDashboardKeys);
  };

  const handleSaveSelectedDashboards = () => {
    handleOUChanges("dashboards", selectedDashboards);
  };

  const handleSaveDeSelectedDashboards = () => {
    setSelectedDashboards(selectedDashboards.filter(dash => !deSelectedDashboards.includes(dash.dashboard_id)));
    setDeSelectedDashboards([]);
    handleOUChanges(
      "dashboards",
      orgUnit?.dashboards?.filter(dash => !deSelectedDashboards.includes(dash.dashboard_id))
    );
  };

  const availableDashboardsRowSelection = useMemo(() => {
    return {
      selectedRowKeys: selectedDashboards.map(dash => dash.dashboard_id),
      onChange: handleSelectedDashboards,
      getCheckboxProps: (rec: any) => {
        return {
          disabled: !![...inheritedDashboards, ...orgUnit.dashboards].find(
            dash => dash.dashboard_id.toString() === rec.id.toString()
          )
        };
      }
    };
  }, [selectedDashboards, orgUnit, inheritedDashboards]);

  const associatedDashboardsRowSelection = useMemo(() => {
    return {
      selectedRowKeys: deSelectedDashboards,
      onChange: handleDeSelectedDashboards,
      getCheckboxProps: (rec: any) => {
        return {
          disabled: !!inheritedDashboards.find(dash => dash.dashboard_id.toString() === rec.dashboard_id.toString())
        };
      }
    };
  }, [deSelectedDashboards, orgUnit, inheritedDashboards]);

  const disableSelectButton = useMemo(() => {
    let disable = true;
    for (let i = 0; i < selectedDashboards.length; i++) {
      const cur = selectedDashboards[i];
      if (!orgUnit?.dashboards.find(d => d.dashboard_id === cur.dashboard_id)) {
        disable = false;
        break;
      }
    }
    return disable;
  }, [selectedDashboards, orgUnit.dashboards]);

  const handleOnAddDashboardClick = () => {
    dispatch(setOUHeaderSelectedWorkspace(SELECTED_OU_HEADER_WORKSPACE, ou_workspace_id as string));
    history.push(`${getDashboardsPage(projectParams)}/create`);
  };

  const availableDashboardColumnConfig = useMemo(
    () => [
      {
        ...baseColumnConfig("Name", "name", { width: "90%" }),
        render: (item, rec) => <AntText>{get(rec, ["metadata", "display_name"], item)}</AntText>
      } as ColumnProps<any>
    ],
    []
  );

  if (dashboardListLoading) {
    return (
      <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
        <Spin />
      </div>
    );
  }

  return (
    <div className="ou-dashboard-configuration-container">
      <Typography.Title level={4}>INSIGHTS</Typography.Title>
      <p className="description">Associate insights with this collection.</p>
      {showNoDashboardsView ? (
        <div className="flex align-center justify-center direction-column w-100p">
          <div className="mb-10">No insights are available to associate with this Collection.</div>
          <AntButton type="primary" onClick={handleOnAddDashboardClick}>
            Add an Insight
          </AntButton>
        </div>
      ) : (
        <div className="flex align-center w-100p">
          <OrgUnitAvailableDashboardsContainer
            rowSelection={availableDashboardsRowSelection}
            title="Available Insights"
            columns={availableDashboardColumnConfig}
          />
          <div className="flex direction-column align-center justify-center ml-20 mr-20">
            <AntButton
              type="primary"
              className="mb-10"
              disabled={disableSelectButton}
              onClick={handleSaveSelectedDashboards}>
              <AntIcon type="right" />
            </AntButton>
            <AntButton type="primary" disabled={!deSelectedDashboards.length} onClick={handleSaveDeSelectedDashboards}>
              <AntIcon type="left" />
            </AntButton>
          </div>
          <OrgUnitAssociatedDashboardsContainer
            rowSelection={associatedDashboardsRowSelection}
            orgUnit={orgUnit}
            inheritedDashboards={inheritedDashboards}
            dashboardListLoading={inheritedDashboardListLoading}
            associatedDashboards={orgUnit?.dashboards}
            handleOUChanges={handleOUChanges}
          />
        </div>
      )}
    </div>
  );
};

export default OrgUnitDashboardConfigurationContainer;
