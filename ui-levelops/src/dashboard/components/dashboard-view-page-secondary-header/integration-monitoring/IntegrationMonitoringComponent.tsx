import React, { useEffect, useMemo, useState } from "react";
import { Dropdown, Icon, Menu, Spin, Typography } from "antd";
import "./integrationMonitoring.styles.scss";
import { RestDashboard } from "classes/RestDashboards";
import { forEach, get, isEqual } from "lodash";
import { ServerPaginatedTable } from "shared-resources/containers";
import { INTEGRATION_MONITORING_KEY } from "dashboard/components/dashboard-settings-modal/constant";
import {
  INTEGRATION_MONITORING_COLUMNS,
  INTEGRATION_STATUSES,
  INTEGRATION_STATUS_PRIORITY_MAPPING,
  MONITORED_INTEGRATION_LIST_UUID
} from "./constant";
import { useDispatch, useSelector } from "react-redux";
import { integrationsListState } from "reduxConfigs/selectors/integrationSelectors";
import { integrationMonitoringAction } from "reduxConfigs/actions/restapi/ingestion.action";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { orgUnitGetDataSelect, orgUnitGetRestDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { OrganizationUnitGet } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { WebRoutes } from "routes/WebRoutes";
import { useHistory, useLocation } from "react-router-dom";
import { usePrevious } from "shared-resources/hooks/usePrevious";
import queryString from "query-string";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ORGANIZATION_UNIT_NODE } from "configurations/pages/Organization/Constants";

interface IntegrationMonitoringProps {
  dashboard: RestDashboard;
}
const IntegrationMonitoringComponent: React.FC<IntegrationMonitoringProps> = (props: IntegrationMonitoringProps) => {
  const { dashboard } = props;
  const [reload, setReload] = useState<number>(1);
  const [orgUnitLoading, setOrgUnitLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const { OU } = queryString.parse(location.search);
  const integrationListState = useSelector(state => integrationsListState(state, MONITORED_INTEGRATION_LIST_UUID));
  const orgUnitId = OU ?? (dashboard?.ou_ids?.length ? dashboard?.ou_ids[0] : "");
  const orgUnit: RestOrganizationUnit = useParamSelector(orgUnitGetRestDataSelect, {
    id: orgUnitId
  });
  const orgUnitGetState = useParamSelector(orgUnitGetDataSelect, {
    id: orgUnitId
  });

  const ouIdRef = usePrevious(orgUnitId);

  const integrationIds = useMemo(() => {
    if (dashboard) {
      const sections = get(orgUnit, ["sections"], []);
      if (orgUnitId && sections.length > 0) {
        let newIds: any = [];
        if (orgUnit?.id) {
          forEach(orgUnit?.sections, section => {
            if ("type" in section && !!section.type) {
              /**
               * Example of section.type is jira@3371
               * where jira is the integration type, 3371 is the integration id
               */

              let typeAsArray = section.type.split("@");
              if (typeAsArray.length === 2) {
                newIds.push(typeAsArray[1]);
              }
            }
          });
        }
        return newIds;
      }
      return get(dashboard.query, ["integration_ids"], []);
    }
    return [];
  }, [dashboard, orgUnit, orgUnitId]);

  const fetchData = () => {
    dispatch(
      genericList(
        "integrations",
        "list",
        { filter: { integration_ids: integrationIds } },
        null,
        MONITORED_INTEGRATION_LIST_UUID
      )
    );
  };

  useEffect(() => {
    if (ouIdRef && !isEqual(ouIdRef, orgUnitId)) {
      dispatch(restapiClear("integrations", "list", "-1"));
    }
    if (ouIdRef && !orgUnitId) {
      fetchData();
    }
  }, [orgUnitId, ouIdRef]);

  useEffect(() => {
    if (orgUnitId && orgUnit && !orgUnit?.id && !orgUnitLoading) {
      setOrgUnitLoading(true);
      dispatch(OrganizationUnitGet(orgUnitId));
    }
  }, [dashboard, orgUnit, orgUnitId, orgUnitLoading]);

  useEffect(() => {
    if (orgUnitLoading) {
      const dataExists = get(orgUnitGetState, ["data", "id"], "");
      const error = get(orgUnitGetState, ["error"], false);
      if (dataExists || error) {
        if (error) {
          orgUnit.id = "-1";
          dispatch(genericRestAPISet(orgUnit?.json, ORGANIZATION_UNIT_NODE, "get", orgUnitId));
        }
        setOrgUnitLoading(false);
      }
    }
  }, [orgUnitGetState, orgUnitLoading, orgUnit, orgUnitId]);

  useEffect(() => {
    if (integrationIds.length) {
      fetchData();
    }
    return () => {
      dispatch(restapiClear("integrations", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    if (orgUnit?.id) {
      fetchData();
    }
  }, [orgUnit, integrationIds]);

  useEffect(() => {
    const loading = get(integrationListState, ["loading"], true);
    const error = get(integrationListState, ["error"], true);
    if (!loading && !error) {
      forEach(integrationIds, id => {
        dispatch(integrationMonitoringAction(id));
      });
    }
  }, [integrationListState, integrationIds]);

  const getDashboardHealthStatus = () => {
    if (integrationIds?.length) {
      const integrations = get(integrationListState, ["data", "records"], []);
      let leastHealth = 100;
      forEach(integrations, (integration: { status: string }) => {
        if (integration?.status) {
          const healthPriority = get(INTEGRATION_STATUS_PRIORITY_MAPPING, [integration?.status.toLowerCase()]);
          if (healthPriority && healthPriority < leastHealth) {
            leastHealth = healthPriority;
          }
        }
      });
      if (leastHealth === 100) return "";
      return INTEGRATION_STATUSES[leastHealth - 1];
    }
    return "";
  };

  const handleViewAllClick = () => {
    history.push(`${WebRoutes.integration.list()}?tab=your_integrations`);
  };

  const handleVisibleChange = (visible: boolean) => {
    if (!visible) {
      dispatch(restapiClear("ingestion_integration_logs", "list", "-1"));
      dispatch(restapiClear("ingestion_integration_status", "get", "-1"));
    } else if (!get(integrationListState, ["loading"], true)) {
      setReload(prev => prev + 1);
    }
  };

  const dashboardHealthStatus = getDashboardHealthStatus();
  return (
    <div className="integration-monitoring-container">
      <Dropdown
        overlay={
          <Menu>
            <Menu.Item className="integration-monitoring-item-container">
              {integrationIds?.length ? (
                <ServerPaginatedTable
                  pageName={INTEGRATION_MONITORING_KEY}
                  uri={"integrations"}
                  className="integration-monitoring-table"
                  hasSearch={false}
                  hasPagination={false}
                  uuid={MONITORED_INTEGRATION_LIST_UUID}
                  title={"Integrations"}
                  reload={reload}
                  moreFilters={{ integration_ids: integrationIds }}
                  columns={INTEGRATION_MONITORING_COLUMNS}
                  hasFilters={false}
                  customExtraContent={
                    <span className="view-all-text" onClick={handleViewAllClick}>
                      View All
                    </span>
                  }
                />
              ) : (
                "No integrations available"
              )}
            </Menu.Item>
          </Menu>
        }
        onVisibleChange={handleVisibleChange}
        placement="bottomCenter"
        className="integration-monitoring-dropdown"
        trigger={["click"]}>
        <Typography.Text>
          <Icon type="api" style={{ color: "var(--grey3)" }} className="ml-10" />
        </Typography.Text>
      </Dropdown>
    </div>
  );
};

export default IntegrationMonitoringComponent;
