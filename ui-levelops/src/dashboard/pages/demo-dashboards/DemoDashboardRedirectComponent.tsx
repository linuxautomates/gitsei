import React, { useEffect, useState } from "react";
import queryString from "query-string";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { DEMO_DASHBOARD_CONFIG_ID } from "./constant";
import { forEach, get, upperCase } from "lodash";
import { configsList } from "reduxConfigs/actions/restapi";
import { Spin } from "antd";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";

const DemoDashboardRedirectComponent: React.FC = () => {
  const location = useLocation();
  const history = useHistory();
  const dispatch = useDispatch();
  const [configLoading, setConfigLoading] = useState<boolean>(false);
  const projectParams = useParams<ProjectPathProps>();

  useEffect(() => {
    dispatch(configsList({}, DEMO_DASHBOARD_CONFIG_ID));
    setConfigLoading(true);
  }, []);

  const configListState = useParamSelector(getGenericUUIDSelector, {
    uri: "configs",
    method: "list",
    uuid: DEMO_DASHBOARD_CONFIG_ID
  });

  const { ou_name, dashboard_name } = queryString.parse(location.search);

  useEffect(() => {
    if (configLoading) {
      const loading = get(configListState, ["loading"], true);
      const error = get(configListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records: Array<{ name: string; value: string }> = get(configListState, ["data", "records"], []);
          if (records.length) {
            let orgUnitRefId = "",
              dashboardId = "";
            forEach(records, (rec: { name: string; value: string }) => {
              const allNames: string[] = rec?.name?.split("##");
              const lastRec: string = allNames[allNames.length - 1];
              if (upperCase(lastRec) === ou_name) {
                orgUnitRefId = rec?.value;
              } else if (lastRec === dashboard_name) {
                dashboardId = rec?.value;
              }
            });
            if (orgUnitRefId && dashboardId) {
              const search = `?OU=${orgUnitRefId}`;
              history.push({
                pathname: `${getDashboardsPage(projectParams)}/${dashboardId}`,
                search
              });
            }
          }
        }
        setConfigLoading(false);
      }
    }
  }, [configListState, configLoading]);

  return (
    <div className="w-100p h-100p flex align-center justify-center">
      <Spin />
    </div>
  );
};

export default DemoDashboardRedirectComponent;
