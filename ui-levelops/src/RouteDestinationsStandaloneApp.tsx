import React from "react";
import { IntegrationsOauthCallback } from "configurations/pages/integrations";
import {
  SIGN_IN_PAGE,
  WORKSPACE_ADD_PATH,
  WORKSPACE_EDIT_PATH,
  WORKSPACE_PATH,
  getBaseUrl
} from "constants/routePaths";
import withTracker from "hoc/withTracker";
import AdminStandaloneAppLayout from "layouts/AdminStandaloneApp";
import AuthLayout from "layouts/Auth.jsx";
import SigninLayout from "layouts/SigninLayout";
import { BrowserRouter, Redirect, Route, Switch } from "react-router-dom";
import AuthCallback from "views/Pages/AuthCallback";
import envConfig from "env-config";
import ReduxStoreProvider from "reduxConfigs/ReduxStoreProvider";

const NODE_ENV = envConfig.get("NODE_ENV");
console.info(`[Running ${NODE_ENV} mode]`);
if (NODE_ENV !== "development") {
  console.log = () => {};
}

const TrackedAdminLayout = withTracker(AdminStandaloneAppLayout);

const routes = (
  <ReduxStoreProvider>
    <BrowserRouter>
      <Switch>
        <Route path="/integration-callback" component={IntegrationsOauthCallback} />
        <Route path="/auth-callback" component={AuthCallback} />
        <Route path="/signin" component={SigninLayout} />
        <Redirect from="/login" to={SIGN_IN_PAGE} />
        <Route path="/auth" render={props => <AuthLayout {...props} />} />
        <Route path={getBaseUrl()} render={props => <TrackedAdminLayout {...props} />} />
        <Redirect from="/products/add-product-page" to={WORKSPACE_ADD_PATH} />
        <Redirect from="/products/edit-product-page" to={WORKSPACE_EDIT_PATH} />
        <Redirect from="/products" to={WORKSPACE_PATH} />
        <Redirect exact from="/" to={getBaseUrl()} />
      </Switch>
    </BrowserRouter>
  </ReduxStoreProvider>
);

export default routes;
