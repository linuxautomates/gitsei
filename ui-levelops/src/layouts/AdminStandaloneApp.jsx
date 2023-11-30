import React, { Component, Suspense } from "react";
import { Layout } from "antd";
import { bugsnagClient } from "bugsnag";
import { Logout } from "components";
import FixedPlugin from "components/FixedPlugin/FixedPlugin.jsx";
import { Sidebar } from "core/components";
import { Header } from "core/containers";
import { get, isEmpty } from "lodash";
import queryString from "query-string";
import "perfect-scrollbar/css/perfect-scrollbar.css";
import IdleTimer from "react-idle-timer";
// this is used to create scrollbars on windows devices like the ones from apple devices
import { connect } from "react-redux";
import { Redirect, Route, Switch } from "react-router-dom";
import { configsState } from "reduxConfigs/selectors/restapiSelector";
import AuthService from "services/authService.js";
import {
  DEFAULT_ROUTES,
  getHomePage,
  getBaseUrl,
  TRELLIS_SCORE_PROFILE_ROUTES,
  getWorkflowProfilePage,
  getSettingsPage,
  getInvestmentPage
} from "../constants/routePaths";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapSessionDispatchtoProps, mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
// dynamically create dashboard routes
import routes from "../routes/routes";
import memoizeOne from "memoize-one";
import { ANT_LAYOUT_CONTENT_ELEMENT_ID } from "constants/elementIds";
import Loader from "../components/Loader/Loader";
import ErrorFallbackComponent from "components/error-boundary-fallback/ErrorBoundaryFallbackComponent";
import classNames from "classnames";
import { isDashboardViewPage, isScoreDashboard } from "utils/dashboardUtils";
import { isIntegrationMapping } from "utils/routeUtils";
import "./admin.scss";
import { sessionEntitlements } from "reduxConfigs/actions/sessionActions";
import { sessionUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { isWorkspaceNeeded, validateUrl } from "./helper";

const { Content } = Layout;

const IDLE_TIMEOUT = 60 * 60 * 1000 * 3; // 3 hours

const JIRA_WIDGETS_ENABLED = "JIRA_WIDGETS_ENABLED";

const ErrorBoundary = bugsnagClient.getPlugin("react").createErrorBoundary();

class AdminStandaloneApp extends Component {
  constructor(props) {
    super(props);
    this.authService = new AuthService();
    this.state = {
      _notificationSystem: null,
      image: null,
      color: "black",
      hasImage: true,
      navbar: false,
      mini: false,
      fixedClasses: "dropdown show-dropdown open",
      mounted: false,
      configLoading: true,
      enableDashboardRoutes: true,
      secondaryDashboardOpened: false,
      currentRoute: "",
      loadingUser: true,
      hasWorkspace: true,
      failedToLoadCurrentUser: false
    };

    this.onIdle = this._onIdle.bind(this);
    this.onActive = this._onActive.bind(this);
    this.onGoToProfileHandler = this.onGoToProfileHandler.bind(this);
    this.previousEntitlements = [];
  }

  componentDidMount() {
    const { workspace_id } = queryString.parse(this.props.location?.search);
    this.props.sessionGetMe();
    this.props.setSessionWorkspace(workspace_id); // setting location params workspace_id is source of truth for dashboard page
    const localUrl = this.props.location?.pathname?.concat(this.props.location?.search);
    if (localUrl !== getBaseUrl()) {
      const inValidURL = validateUrl(this.props.location);
      if (!inValidURL) {
        localStorage.setItem("prev_location", this.props.location.pathname);
        localStorage.setItem("prev_search", this.props.location.search);
      } else {
        localStorage.setItem("prev_location", getHomePage());
        localStorage.setItem("prev_search", "");
      }
    }
    bugsnagClient.addMetadata("appMode", "standalone");
    bugsnagClient.addMetadata("company", this.props.session_company);
    bugsnagClient.addMetadata("username", this.props.session_username);
    const user = {
      id: this.props.session_company,
      name: "",
      email: this.props.session_username
    };
    bugsnagClient.setUser(user);
    this.props.configsList({ filter: { name: JIRA_WIDGETS_ENABLED } });
  }

  componentDidUpdate(e) {
    const { search } = this.props.location;

    const inValidURL = validateUrl(this.props.location);
    const localUrl = this.props.location?.pathname?.concat(this.props.location?.search);
    if (inValidURL && localUrl !== getBaseUrl()) {
      this.props.history.replace(getHomePage());
    } else if (localUrl === getBaseUrl() && !this.state.loadingUser) {
      const lastLoginURL = get(this.props.sessionCurrentUser, ["data", "metadata", "last_login_url"], getHomePage());
      this.props.history.replace(lastLoginURL);
    }

    const { entitlements } = queryString.parse(search);
    if (entitlements) {
      const entArray = entitlements.split(",");
      if (JSON.stringify(this.previousEntitlements) != JSON.stringify(entArray)) {
        this.previousEntitlements = entArray;
        this.props.setSessionEntitlements(entArray);
      }
    }
    if (e.history.action === "PUSH") {
      document.documentElement.scrollTop = 0;
      document.scrollingElement.scrollTop = 0;
      if (this.refs.mainPanel && this.refs.mainPanel.scrollTop) {
        this.refs.mainPanel.scrollTop = 0;
      }
    }
    if (
      window.innerWidth < 993 &&
      e.history.action === "PUSH" &&
      document.documentElement.className.indexOf("nav-open") !== -1
    ) {
      document.documentElement.classList.toggle("nav-open");
    }
  }

  componentWillMount() {
    if (document.documentElement.className.indexOf("nav-open") !== -1) {
      document.documentElement.classList.toggle("nav-open");
    }
  }

  static getDerivedStateFromProps(props, state) {
    if (state.configLoading) {
      const { loading, error } = props.configs;

      if (!loading && !error) {
        const records = get(props, ["configs", "data", "records"], []);

        if (isEmpty(records)) {
          return {
            ...state,
            configLoading: false,
            enableDashboardRoutes: true
          };
        }

        const jiraDash = records.find(rec => rec.name === JIRA_WIDGETS_ENABLED);
        return {
          ...state,
          configLoading: false,
          enableDashboardRoutes: jiraDash?.value === "true"
        };
      }
    }

    const { sessionCurrentUser } = props;
    const loading = get(sessionCurrentUser, "loading", true);
    const error = get(sessionCurrentUser, "error", true);
    if (!loading) {
      let newAdditionalState = { loadingUser: false };
      if (!error) {
        const selectedWorkspace = get(sessionCurrentUser, ["data", "metadata", "selected_workspace"], undefined);
        if (!selectedWorkspace) {
          const workspaceNeeded = isWorkspaceNeeded(props.history.location);
          if (workspaceNeeded) {
            props.history.push(getHomePage());
            newAdditionalState = {
              ...newAdditionalState,
              hasWorkspace: false
            };
          } else {
            newAdditionalState = {
              ...newAdditionalState,
              hasWorkspace: true
            };
          }
        }
      } else {
        newAdditionalState = {
          ...newAdditionalState,
          failedToLoadCurrentUser: true
        };
      }
      return {
        ...(state ?? {}),
        ...(newAdditionalState ?? {})
      };
    }

    return null;
  }

  addClass = () => {
    if (this.props.location.pathname === `${getSettingsPage()}/workflow-editor`) {
      return "page-inner-container h-100";
    }
    return "page-inner-container";
  };

  // function that shows/hides notifications - it was put here, because the wrapper div has to be outside the main-panel class div
  handleNotificationClick = position => {
    var color = Math.floor(Math.random() * 4 + 1);
    // eslint-disable-next-line no-unused-vars
    var level;
    switch (color) {
      case 1:
        level = "success";
        break;
      case 2:
        level = "warning";
        break;
      case 3:
        level = "error";
        break;
      case 4:
        level = "info";
        break;
      default:
        break;
    }
  };

  handleImageClick = image => {
    this.setState({ image: image });
  };
  handleColorClick = color => {
    this.setState({ color: color });
  };
  handleHasImage = hasImage => {
    this.setState({ hasImage: hasImage });
  };
  handleNavbarClick = navbar => {
    this.setState({ navbar: navbar });
  };
  handleMiniClick = () => {
    this.setState({ mini: !this.state.mini });
    document.body.classList.toggle("sidebar-mini");
  };
  handleFixedClick = () => {
    if (this.state.fixedClasses === "dropdown") {
      this.setState({ fixedClasses: "dropdown show-dropdown open" });
    } else {
      this.setState({ fixedClasses: "dropdown" });
    }
  };

  get routes() {
    return routes({
      hideDashboards: !this.state.enableDashboardRoutes
    });
  }

  get adminRoutes() {
    if (this.state.loadingUser) {
      return [];
    }
    const adminRoutes = this.routes
      .filter(
        route =>
          !route.hide && route.layout === getBaseUrl() && route.rbac.includes(this.props.session_rbac.toLowerCase())
      )
      .filter(
        f => (!this.state.enableDashboardRoutes && !f.path.match(/dashboards/g)) || this.state.enableDashboardRoutes
      )
      .map((route, key) => (
        <Route
          path={route.layout + route.path}
          exact={route.exact !== false}
          key={key}
          render={routeProps => {
            return (
              <route.component
                {...routeProps}
                session_rbac={this.props.session_rbac}
                enableDashboardRoutes={this.state.enableDashboardRoutes}
                handleClick={this.handleNotificationClick}
              />
            );
          }}
        />
      ));

    if (this.authService.loggedIn(this.props.session_token)) {
      adminRoutes.push(
        <Route
          key="default-route-key"
          path={`${getBaseUrl()}/*`}
          render={() => <Redirect to={this.defaultRoute} noThrow={true} />}
        />
      );
    }

    return adminRoutes;
  }

  _onIdle(e) {
    this.props.restapiClearAll();
    this.authService.logout();
    this.props.sessionLogout();
  }

  _onActive(e) {}

  onGoToProfileHandler() {
    this.props.history.push(`${getSettingsPage()}/user-page`);
  }

  get defaultRoute() {
    const rbac = this.props.session_rbac;
    //console.log(`rbac is ${rbac}`);
    if (
      this.props.session_default_route === undefined ||
      this.props.session_default_route === null ||
      this.props.session_default_route === "undefined"
    ) {
      console.log("session default route not yet set");
      return DEFAULT_ROUTES[rbac]?.();
    }
    return this.props.session_default_route || DEFAULT_ROUTES[rbac]?.();
  }

  memoizedPage = memoizeOne(pathname => {
    return this.routes.find(route => !!route.path && pathname.includes(route.layout + route.path));
  });

  render() {
    if (!this.authService.loggedIn(this.props.session_token)) {
      this.props.restapiClearAll();
      this.authService.logout();
      this.props.sessionLogout();
      const sso_error = localStorage.getItem("sso_redirect_error");
      return sso_error?.length ? "" : <Logout />;
    }

    if (this.state.loadingUser || !this.state.hasWorkspace) {
      return <Loader />;
    }

    const page = this.memoizedPage(this.props.location.pathname);

    if (this.state.currentRoute !== this.props.location.pathname) {
      this.setState({
        currentRoute: this.props.location.pathname,
        secondaryDashboardOpened: false
      });
    }

    if (this.state.failedToLoadCurrentUser) {
      return <Logout />;
    }

    const isDevProductivityRoute = () => {
      return window.location.href.includes(TRELLIS_SCORE_PROFILE_ROUTES._ROOT);
    };

    const isWorkflowProfileRoute = () => {
      return window.location.href.includes(getWorkflowProfilePage());
    };

    const isInvestmentProfileRoute = () => {
      return window.location.href.includes(getInvestmentPage());
    };

    return (
      <ErrorBoundary FallbackComponent={ErrorFallbackComponent}>
        <Layout>
          <IdleTimer
            ref={ref => {
              this.idleTimer = ref;
            }}
            element={document}
            onIdle={this.onIdle}
            onActive={this.onActive}
            debounce={250}
            timeout={IDLE_TIMEOUT}
          />
          <Sidebar
            history={this.props.history}
            location={this.props.location}
            rbac={this.props.session_rbac.toLowerCase()}
            onLogoutEvent={this.props.sessionLogout}
            onNavigateToProfileEvent={this.onGoToProfileHandler}
            enableDashboardRoutes={this.state.enableDashboardRoutes}
          />
          <Layout
            className={classNames(
              { "dashboard-view-page-container": isDashboardViewPage() || isScoreDashboard() },
              { "dev-productivity-container": isDevProductivityRoute() || isIntegrationMapping() },
              { "workflow-profile-container": isWorkflowProfileRoute() || isInvestmentProfileRoute() }
            )}>
            <Header
              enableDashboardRoutes={this.state.enableDashboardRoutes}
              page={page}
              location={this.props.location}
              showToggleMenu={page && !!page.secondaryHeader}
              toggleMenu={() =>
                this.setState(state => {
                  return {
                    ...state,
                    secondaryDashboardOpened: !state.secondaryDashboardOpened
                  };
                })
              }
              history={this.props.history}
            />
            <Content id={ANT_LAYOUT_CONTENT_ELEMENT_ID}>
              <div className="page-container" ref="mainPanel">
                <Suspense fallback={<Loader />}>
                  <Switch>{this.adminRoutes}</Switch>
                </Suspense>
                <FixedPlugin
                  handleImageClick={this.handleImageClick}
                  handleColorClick={this.handleColorClick}
                  handleHasImage={this.handleHasImage}
                  handleNavbarClick={this.handleNavbarClick}
                  handleMiniClick={this.handleMiniClick}
                  bgColor={this.state["color"]}
                  bgImage={this.state["image"]}
                  mini={this.state["mini"]}
                  handleFixedClick={this.handleFixedClick}
                  fixedClasses={this.state.fixedClasses}
                />
              </div>
            </Content>
          </Layout>
        </Layout>
      </ErrorBoundary>
    );
  }
}

const mapStateToProps = state => {
  return {
    ...mapSessionStatetoProps(state),
    configs: configsState(state),
    sessionCurrentUser: sessionUserState(state)
  };
};

const mapDispatch = dispatch => ({
  ...mapSessionDispatchtoProps(dispatch),
  ...mapRestapiDispatchtoProps(dispatch),
  setSessionEntitlements: entitlements => dispatch(sessionEntitlements(entitlements))
});

export default connect(mapStateToProps, mapDispatch)(AdminStandaloneApp);
