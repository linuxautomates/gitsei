import React from "react";
import { Component } from "react";
import { connect } from "react-redux";
import queryString from "query-string";
import Cookies from "js-cookie";
import { mapSessionStatetoProps, mapSessionDispatchtoProps } from "reduxConfigs/maps/sessionMap";
import { ERROR } from "../../constants/localStorageKeys";
import { BASE_UI_URL } from "helper/envPath.helper";
import { getBaseUrl, SIGN_IN_PAGE } from "constants/routePaths";
import { sessionUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { getLocationObject } from "./helper";
import { validateUrl } from "layouts/helper";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { get } from "lodash";
import Loader from "components/Loader/Loader";

const URL = BASE_UI_URL;

class AuthCallback extends Component {
  constructor(props) {
    super(props);
    let prevQuery = localStorage.getItem("prev_search");
    let prevLocation = localStorage.getItem("prev_location");
    this.state = { loading: true, success: false, prev_search: prevLocation, prev_query: prevQuery };

    this.navigationURL = this.navigationURL.bind(this);
  }

  navigationURL(url, locationObject, selectedWorkspace) {
    const inValidURL = validateUrl(locationObject);
    if (selectedWorkspace) {
      const selected_workspace = selectedWorkspace;
      this.props.setSelectedWorkspace(selected_workspace);
    }
    if (inValidURL) {
      window.location.replace(URL.concat(getBaseUrl()));
    } else {
      window.location.replace(URL.concat(url));
    }
  }

  componentWillReceiveProps(nextProps, nextContext) {
    const sessionUserLoading = get(nextProps.sessionCurrentUser, "loading", true);
    const sessionUserError = get(nextProps.sessionCurrentUser, "error", true);
    if (!sessionUserLoading && !sessionUserError) {
      if (nextProps.session_token !== null) {
        if (!nextProps.session_error) {
          if (nextProps.session_user_id !== null) {
            if (this.state.prev_search !== undefined && this.state.prev_search !== null) {
              let url = this.state.prev_search;
              if (this.state.prev_query !== undefined && this.state.prev_query !== null) {
                let prevQuery = this.state.prev_query;
                url = url.concat(prevQuery);
                localStorage.removeItem("prev_search");
                localStorage.removeItem("prev_location");
              }
              window.location.replace(URL.concat(url));
            } else if (
              nextProps?.sessionCurrentUser?.data?.metadata?.last_login_url &&
              nextProps?.sessionCurrentUser?.data?.metadata?.selected_workspace
            ) {
              const lastLoginUrl = get(nextProps?.sessionCurrentUser, ["data", "metadata", "last_login_url"], "");
              const locationObject = getLocationObject(lastLoginUrl);
              const selectedWorkspace = get(
                nextProps?.sessionCurrentUser,
                ["data", "metadata", "selected_workspace"],
                undefined
              );
              this.navigationURL(lastLoginUrl, locationObject, selectedWorkspace);
            } else {
              // TODO use the default route here
              const defaultRoute = nextProps.session_default_route;
              if (defaultRoute !== undefined && defaultRoute !== null) {
                console.log(`redirecting to default route ${defaultRoute}`);
                window.location.replace(URL.concat(defaultRoute));
              }
            }
          }
        } else {
          this.props.sessionError("SSO Error");
          window.location.replace(URL.concat(SIGN_IN_PAGE));
        }
      }
    } else if (!sessionUserLoading && sessionUserError) {
      this.props.sessionError("SSO Error");
      window.location.replace(URL.concat(SIGN_IN_PAGE));
    }
  }

  componentDidMount() {
    const redirectError = queryString.parse(this.props.location.search).error;
    if (redirectError) {
      localStorage.setItem("sso_redirect_error", redirectError);
    }
    let jwtToken = Cookies.get("token");
    if (jwtToken === undefined) {
      const values = queryString.parse(this.props.location.search);
      let errMsg = values.error || "SSO Error";
      this.props.sessionError(errMsg);
      localStorage.setItem(ERROR, errMsg);
      console.log(errMsg);
      console.log("removing cookie");
      Cookies.remove("token", { path: "" });
      Cookies.remove("token");
      window.location.replace(URL.concat(SIGN_IN_PAGE));
    }
    this.props.sessionSSOLogin(jwtToken);
  }

  render() {
    return <Loader />;
  }
}

const mapStateToProps = state => {
  return {
    ...mapSessionStatetoProps(state),
    sessionCurrentUser: sessionUserState(state)
  };
};

const mapDispatch = dispatch => ({
  ...mapSessionDispatchtoProps(dispatch),
  setSelectedWorkspace: selected_workspace => dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, selected_workspace))
});

export default connect(mapStateToProps, mapDispatch)(AuthCallback);
