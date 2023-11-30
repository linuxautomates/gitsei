import React, { Component } from "react";
import { Layout } from "antd";
import { bugsnagClient } from "bugsnag";
import { Header } from "core/containers";
import { get, isEmpty } from "lodash";
import queryString from "query-string";
import "perfect-scrollbar/css/perfect-scrollbar.css";
// this is used to create scrollbars on windows devices like the ones from apple devices
import { connect } from "react-redux";
import { configsState } from "reduxConfigs/selectors/restapiSelector";
import AuthService from "services/authService.js";
import { TRELLIS_SCORE_PROFILE_ROUTES, getWorkflowProfilePage, getInvestmentPage } from "../constants/routePaths";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapSessionDispatchtoProps, mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
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
import routes from "../routes/routes";
import HarnessLogin from "./HarnessLogin";
import HarnessRoutes from "./HarnessRoutes";
import "./AdminHarness.scss";

const { Content } = Layout;

const JIRA_WIDGETS_ENABLED = "JIRA_WIDGETS_ENABLED";

const ErrorBoundary = bugsnagClient.getPlugin("react").createErrorBoundary();

class AdminHarness extends Component {
  constructor(props) {
    super(props);
    this.authService = new AuthService();
    this.state = {
      configLoading: true,
      enableDashboardRoutes: true,
      secondaryDashboardOpened: false,
      currentRoute: "",
      hasWorkspace: true
    };
    this.previousEntitlements = [];
  }

  componentDidMount() {
    bugsnagClient.addMetadata("appMode", "HarnessIntegration");
    bugsnagClient.addMetadata("company", this.props.session_company);
    bugsnagClient.addMetadata("username", this.props.session_username);
    const user = {
      id: this.props.session_company,
      name: "",
      email: this.props.session_username
    };
    bugsnagClient.setUser(user);
    this.props.configsList({ filter: { name: JIRA_WIDGETS_ENABLED } });

    // Load env-config
    const script = document.createElement("script");
    script.src = `${window.location.origin}/sei/env-config.js`;
    script.async = true;
    script.type = "module";
    document.body.appendChild(script);
  }

  componentDidUpdate(e) {
    const { search } = this.props.location;
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
    return state;
  }

  get routes() {
    return routes({
      hideDashboards: !this.state.enableDashboardRoutes
    });
  }

  memoizedPage = memoizeOne(pathname => {
    return this.routes.find(route => !!route.path && pathname.includes(route.path));
  });

  render() {
    const { isNav2Enabled } = this.props;
    if (!this.state.hasWorkspace) {
      return <Loader />;
    }

    const page = this.memoizedPage(this.props.location.pathname);

    if (this.state.currentRoute !== this.props.location.pathname) {
      this.setState({
        currentRoute: this.props.location.pathname,
        secondaryDashboardOpened: false
      });
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
        <Layout className="admin-harness">
          <Layout
            className={classNames(
              { "dashboard-view-page-container": isDashboardViewPage() || isScoreDashboard() },
              { "dev-productivity-container": isDevProductivityRoute() || isIntegrationMapping() },
              { "workflow-profile-container": isWorkflowProfileRoute() || isInvestmentProfileRoute() },
              { withHarness: true }
            )}>
            <HarnessLogin />
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
                <HarnessRoutes enableDashboardRoutes={this.state.enableDashboardRoutes} isNav2Enabled={isNav2Enabled} />
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

export default connect(mapStateToProps, mapDispatch)(AdminHarness);
