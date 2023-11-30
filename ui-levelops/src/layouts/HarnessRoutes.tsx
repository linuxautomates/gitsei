import React, { useMemo } from "react";
import { Redirect, Route, Switch } from "react-router-dom";
import routes from "../routes/routes";
import { getBaseUrl } from "constants/routePaths";
import { useAppStore } from "contexts/AppStoreContext";
import RedirectToSEIModule from "./RedirectToSEIModule";
import { useParentProvider } from "contexts/ParentProvider";
import { projectPathPropsDef } from "utils/routeUtils";
import { isAccountSelectedFromNoScopeState, isProjectSelectedFromNoScopeState } from "./layout.utils";

interface HarnessRoutesProps {
  enableDashboardRoutes: boolean;
  isNav2Enabled?: boolean;
}

const HarnessRoutes = (props: HarnessRoutesProps) => {
  const { isNav2Enabled, enableDashboardRoutes } = props;
  const { baseUrl } = useAppStore();
  const {
    components: { AccessControlRouteDestinations, EmptyLayout }
  } = useParentProvider();
  const currentLocation = window.location.href;

  if (isProjectSelectedFromNoScopeState(currentLocation, isNav2Enabled)) {
    return <Redirect to={`${getBaseUrl()}/dashboards`} />;
  } else if (isAccountSelectedFromNoScopeState(currentLocation, isNav2Enabled)) {
    return <Redirect to={`${getBaseUrl()}/configuration/integrations`} />;
  }

  const routePaths = useMemo(
    () =>
      routes({
        hideDashboards: !enableDashboardRoutes
      })
        .filter(
          (route: any) =>
            !route.hide && (route.layout === getBaseUrl() || route.layout === getBaseUrl(projectPathPropsDef))
        )
        .filter(f => (!enableDashboardRoutes && !f.path.match(/dashboards/g)) || enableDashboardRoutes)
        .map((route, key) => {
          return (
            <Route
              path={route.layout + route.path}
              exact={(route as any).exact !== false}
              key={key}
              render={routeProps => <route.component {...routeProps} enableDashboardRoutes={enableDashboardRoutes} />}
            />
          );
        }),
    [baseUrl]
  );

  return (
    <Switch>
      <Route path={getBaseUrl()} exact={true} key={"basepath"} render={() => <RedirectToSEIModule />} />
      {[
        ...routePaths,
        AccessControlRouteDestinations({
          moduleParams: {
            module: ":module(sei)"
          },
          layout: EmptyLayout
        })
      ]}
    </Switch>
  );
};

export default HarnessRoutes;
