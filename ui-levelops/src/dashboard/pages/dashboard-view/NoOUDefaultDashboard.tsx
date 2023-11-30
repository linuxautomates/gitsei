import { OU_DASHBOARD_LIST_ID } from "configurations/pages/Organization/Constants";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { get, orderBy } from "lodash";
import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { setPageButtonAction } from "reduxConfigs/actions/pagesettings.actions";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntButton, AntCol, AntIcon, AntRow } from "shared-resources/components";
import { CONTACT_MSG, NO_DASHBOARD_MSG } from "views/Pages/landing-page/constant";
import "./NoOUDefaultDashboard.scss";
import { getDashboardsPage, getBaseUrl } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";

const NoOUDefaultDashboard: React.FC<{}> = props => {
  const entDashboard = useHasEntitlements([Entitlement.DASHBOARDS, Entitlement.ALL_FEATURES], EntitlementCheckType.OR);
  const history = useHistory();
  const location = useLocation();
  const dispatch = useDispatch();
  const isNodash = location.pathname === `${getBaseUrl()}/no-ou-dash`;
  const msg = entDashboard ? `${NO_DASHBOARD_MSG}` : `${NO_DASHBOARD_MSG} ${CONTACT_MSG}`;
  const projectParams = useParams<ProjectPathProps>();

  const dashboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_dashboard_list",
    method: "list",
    uuid: OU_DASHBOARD_LIST_ID
  });

  useEffect(() => {
    const nlist = get(dashboardListState, ["data", "records"], []);
    const loading = get(dashboardListState, ["loading"], true);
    const error = get(dashboardListState, ["error"], true);
    if (!loading && !error && isNodash && nlist.length > 0) {
      const orderDash = orderBy(nlist, ["dashboard_order"], ["asc"]);
      let defaultDashboard: any = {};
      orderDash.forEach((element: any) => {
        if (element.is_default) {
          defaultDashboard = element;
        }
      });
      const id = defaultDashboard?.dashboard_id ? defaultDashboard?.dashboard_id : orderDash?.[0]?.dashboard_id;
      history.push({
        pathname: `${getDashboardsPage(projectParams)}/${id}`,
        search: location.search
      });
    }
  }, [dashboardListState, isNodash]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="no-ou-dash">
      <AntRow className="flex">
        <AntCol span={24} className="info">
          <span>
            <AntIcon type="info-circle" />
            <span className="pl-9">{msg}</span>
          </span>
        </AntCol>
      </AntRow>
      <AntRow className="flex">
        {entDashboard && (
          <AntCol span={24}>
            <AntButton
              type="primary"
              onClick={() => {
                dispatch(setPageButtonAction(history.location.pathname, "create_dashboard", { hasClicked: true }));
                history.push(`${getDashboardsPage(projectParams)}/create`);
              }}>
              <span className="pl-5">Add Insight</span>
            </AntButton>
          </AntCol>
        )}
      </AntRow>
    </div>
  );
};

export default React.memo(NoOUDefaultDashboard);
