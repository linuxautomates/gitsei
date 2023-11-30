import React, { Component } from "react";
import { Tabs } from "antd";
import queryString from "query-string";
import TriageRulesListPage from "./triageRulesList.page";
import TriageGridViewPage from "./grid-view/TriageGridView.page";
import { TRIAGE_TABS } from "../../constants/triageParams";
import { connect } from "react-redux";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import "./triageLandingPage.style.scss";
import { buildQueryParamsFromObject } from "../../utils/queryUtils";
import { WebRoutes } from "../../routes/WebRoutes";
import { getBaseUrl } from "constants/routePaths";

const { TabPane } = Tabs;

interface Props {
  page?: any;
  location?: any;
  match?: any;
  history?: any;
  isSelfOnboardingUser?: boolean;
}

interface State {
  current_tab: TRIAGE_TABS;
  triageGridViewFilters: {
    [key: string]: undefined | string | string[] | number | number[];
  };
}
class TriageLandingPage extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      current_tab: TRIAGE_TABS.TRIAGE_GRID_VIEW,
      triageGridViewFilters: {}
    };
    this.setTriageGridViewFilters = this.setTriageGridViewFilters.bind(this);
  }

  componentDidMount() {
    if (this.props.isSelfOnboardingUser) {
      this.props.history.push({ pathname: WebRoutes.dashboard.details(this.props.match.params, "") });
    }
  }
  componentDidUpdate() {
    if (this.props.isSelfOnboardingUser) {
      this.props.history.push({ pathname: WebRoutes.dashboard.details(this.props.match.params, "") });
    }
  }
  static getDerivedStateFromProps(props: any, state: any) {
    const values = queryString.parse(props.location.search);
    const tab = values.tab ? values.tab : TRIAGE_TABS.TRIAGE_GRID_VIEW;
    if (state.current_tab !== tab) {
      return {
        ...state,
        current_tab: tab
      };
    }

    return state;
  }

  setTriageGridViewFilters(filters = {}) {
    this.setState({ triageGridViewFilters: filters });
  }

  get triageGridViewTab() {
    return (
      <TabPane key={TRIAGE_TABS.TRIAGE_GRID_VIEW} tab={"Triage Grid View"}>
        {this.state.current_tab === TRIAGE_TABS.TRIAGE_GRID_VIEW && (
          <TriageGridViewPage
            setTriageFilters={this.setTriageGridViewFilters}
            history={this.props.history}
            location={this.props.location}
          />
        )}
      </TabPane>
    );
  }

  render() {
    return (
      <Tabs
        activeKey={this.state.current_tab}
        animated={false}
        onChange={key => {
          if (key === "triage_grid_view" && Object.keys(this.state.triageGridViewFilters).length > 0) {
            this.props.history.push(
              `${getBaseUrl()}/triage?tab=${key}&${buildQueryParamsFromObject(this.state.triageGridViewFilters)}`
            );
          } else {
            this.props.history.push(`${getBaseUrl()}/triage?tab=${key}`);
          }
        }}
        className={this.state.current_tab === "triage_grid_view" ? "triage-grid-view-tab" : ""}
        size={"small"}>
        {this.triageGridViewTab}
        <TabPane key={TRIAGE_TABS.TRIAGE_RULES} tab={"Triage Rules"}>
          {this.state.current_tab === TRIAGE_TABS.TRIAGE_RULES && <TriageRulesListPage history={this.props.history} />}
        </TabPane>
      </Tabs>
    );
  }
}

export default connect(mapSessionStatetoProps)(TriageLandingPage);
