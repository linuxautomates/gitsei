import React from "react";
import AdminHarness from "layouts/AdminHarness";
import { Route } from "react-router-dom";
import { AppStoreContext } from "contexts/AppStoreContext";
import ReduxStoreProvider from "reduxConfigs/ReduxStoreProvider";

const routes = (
  <ReduxStoreProvider>
    <AppStoreContext.Consumer>
      {({ baseUrl, isNav2Enabled }) => (
        <Route path={baseUrl} render={props => <AdminHarness {...props} isNav2Enabled={isNav2Enabled} />} />
      )}
    </AppStoreContext.Consumer>
  </ReduxStoreProvider>
);

export default routes;
