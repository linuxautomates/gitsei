import React from "react";
import { connect } from "react-redux";
import queryString from "query-string";
import { Redirect } from "react-router-dom";
import { SELF_ONBOARDING_INTEGRATIONS } from "configurations/pages/self-onboarding/constants";
import { WebRoutes } from "routes/WebRoutes";
import { getSettingsPage } from "constants/routePaths";

export class IntegrationsOauthCallbackContainer extends React.PureComponent {
  componentDidMount() {
    if (this.props.location.search) {
      const values = queryString.parse(this.props.location.search);
      const pendingState = JSON.parse(sessionStorage.getItem(values.state));
      if (pendingState) {
        sessionStorage.setItem(values.state, JSON.stringify({ ...pendingState, ...values }));
        if (SELF_ONBOARDING_INTEGRATIONS.includes(pendingState.name)) {
          window.location.replace(
            window.location.origin
              .concat(WebRoutes.self_onboarding.root(pendingState.name, 1))
              .concat("&state=" + values.state)
              .concat("&code=" + values.code)
          );
        } else {
          window.location.replace(
            window.location.origin
              .concat(`${getSettingsPage()}/integrations/add-integration-page?state=`)
              .concat(values.state)
          );
        }
      }
    }
  }

  render() {
    return <Redirect to="/" />;
  }
}

export default connect()(IntegrationsOauthCallbackContainer);
