import React from "react";
import { connect } from "react-redux";
import { Redirect } from "react-router-dom";
import { SIGN_IN_PAGE } from "constants/routePaths";
import { mapSessionDispatchtoProps, mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";

interface LogoutComponentProps {
  sessionLogout: () => {};
}

const LogoutComponent: React.FC<LogoutComponentProps> = props => {
  props.sessionLogout();
  return <Redirect to={SIGN_IN_PAGE} />;
};

export default connect(mapSessionStatetoProps, mapSessionDispatchtoProps)(LogoutComponent);
