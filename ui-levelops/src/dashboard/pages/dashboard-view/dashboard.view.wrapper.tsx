import React, { Component } from "react";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { WidgetType } from "dashboard/helpers/helper";
import { connect } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { DASHBOARD_PAGE_SIZE } from "constants/dashboard";
import DashboardViewContainer from "./dashboard-view.container";
import { get, omit } from "lodash";
import { getWidgetsByDashboardId } from "reduxConfigs/selectors/widgetSelector";
import { mapOrgUnitToProps } from "reduxConfigs/maps/restapi/organizationMap";
import { OrganizationUnitListFilterType } from "reduxConfigs/types/map-types/organizationMap.types";
import { DASHBOARD_OU_CHECK_LIST_ID } from "./constant";
import { dashboardCheckOUListDataState } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { ORGANIZATION_UNIT_NODE } from "configurations/pages/Organization/Constants";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { Spin } from "antd";
import queryString from "query-string";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { getBaseUrl } from "constants/routePaths";
import { getGenericMethodSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import LocalStoreService from "services/localStoreService";
interface DashboardViewWrapperProps extends RouteComponentProps {
  dashboard: RestDashboard;
  widgets: RestWidget[];
  restapiClear: (uri: string, method: string, id?: string | number) => any;
  orgUnitList: (filters: OrganizationUnitListFilterType, id: string) => any;
  orgUnitListState: Array<orgUnitJSONType>;
  selectedWorkspace: WorkspaceModel;
  OUDataState: any;
  orgUnitGet: (id: string) => any;
}

interface DashboardViewWrapperState {
  graphWidgetPage: number;
  statsWidgetPage: number;
  orgUnitListLoading: boolean;
  orgUnitLoading: boolean;
}

class DashboardViewComponentWrapper extends Component<DashboardViewWrapperProps, DashboardViewWrapperState> {
  constructor(props: any) {
    super(props);

    this.state = {
      graphWidgetPage: 0,
      statsWidgetPage: 0,
      orgUnitListLoading: false,
      orgUnitLoading: false
    };
  }

  get statsWidgetCount() {
    const widgets = this.props.widgets || [];
    return widgets.filter((widget: any) => widget.widget_type === WidgetType.STATS).length;
  }

  get graphWidgetCount() {
    const widgets = this.props.widgets || [];
    return widgets.filter((widget: any) => widget.widget_type !== WidgetType.STATS).length;
  }

  componentDidMount() {
    const { OU, workspace_id } = queryString.parse(this.props.location?.search ?? {});
    if (this.props.selectedWorkspace?.id && OU && !workspace_id) {
      const OUData = get(this.props.OUDataState, [OU as string, "data"], {});
      if (Object.keys(OUData || {})?.length && OUData?.ou_id) {
        const ou_id = OUData?.ou_id;
        this.props.orgUnitList?.(
          { filter: { workspace_id: [this.props.selectedWorkspace?.id], ou_id: [ou_id] } },
          DASHBOARD_OU_CHECK_LIST_ID
        );
        this.setState({
          ...(this.state ?? {}),
          orgUnitListLoading: true
        });
      } else {
        this.props.orgUnitGet?.(OU as string);
        this.setState({
          ...(this.state ?? {}),
          orgUnitLoading: true
        });
      }
    }
    if (this.props.selectedWorkspace?.id && workspace_id && this.props.selectedWorkspace?.id !== workspace_id) {
      const ls = new LocalStoreService();
      // @ts-ignore
      this.props.setSelectedWorkspace(SELECTED_WORKSPACE_ID, { id: workspace_id });
      // @ts-ignore
      this.props.setSessionWorkspace(workspace_id);
      ls.setSelectedWorkspaceId(workspace_id || "");
    }
    window.addEventListener("scroll", this.handleScroll, true);
  }

  static getDerivedStateFromProps(props: DashboardViewWrapperProps, state: DashboardViewWrapperState) {
    if (state.orgUnitLoading) {
      const { OU } = queryString.parse(props.location?.search ?? {});
      const loading = get(props.OUDataState, [OU as string, "loading"], true);
      const error = get(props.OUDataState, [OU as string, "error"], true);
      if (!loading && !error) {
        const data = get(props.OUDataState, [OU as string, "data"], {});
        if (data) {
          const ou_id = data?.ou_id;
          props.orgUnitList?.(
            { filter: { workspace_id: [props.selectedWorkspace?.id], ou_id: [ou_id] } },
            DASHBOARD_OU_CHECK_LIST_ID
          );

          return {
            ...(state ?? {}),
            orgUnitListLoading: true,
            orgUnitLoading: false
          };
        }
      }
    }
    if (state.orgUnitListLoading) {
      const loading = get(props.orgUnitListState, ["loading"], true);
      const error = get(props.orgUnitListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records = get(props.orgUnitListState, ["data", "records"], []);
          if (!records.length) {
            props.history.push(getBaseUrl());
          } else {
            props.history.push({
              search: `${props.location.search}&workspace_id=${props.selectedWorkspace?.id}`
            });
          }
        }
        return {
          ...(state ?? {}),
          orgUnitListLoading: false
        };
      }
    }
    return state;
  }

  componentWillUnmount() {
    this.props.restapiClear(ORGANIZATION_UNIT_NODE, "list", DASHBOARD_OU_CHECK_LIST_ID);
    window.removeEventListener("scroll", this.handleScroll, true);
  }

  handleScroll = (event: any) => {
    const { graphWidgetPage, statsWidgetPage } = this.state;
    let maxScroll = event.target.scrollHeight - event.target.clientHeight;
    let currentScroll = event.target.scrollTop;
    if (currentScroll >= maxScroll - 100) {
      if (this.statsWidgetCount < statsWidgetPage * DASHBOARD_PAGE_SIZE) {
        if (this.graphWidgetCount > graphWidgetPage * DASHBOARD_PAGE_SIZE) {
          this.setState({
            graphWidgetPage: graphWidgetPage + 1
          });
        }
      } else {
        this.setState({
          statsWidgetPage: statsWidgetPage + 1
        });
      }
    }
  };

  handleStatsWidgetPage = (page: number) => this.setState({ statsWidgetPage: page });

  handleGraphWidgetPage = (page: number) => this.setState({ graphWidgetPage: page });

  render() {
    if (this.state.orgUnitListLoading || this.state.orgUnitLoading)
      return (
        <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
          <Spin />
        </div>
      );
    return (
      <DashboardViewContainer
        {...omit(this.props, "dashboard")}
        statsWidgetPage={this.state.statsWidgetPage}
        graphWidgetPage={this.state.graphWidgetPage}
        setGraphWidgetPage={this.handleGraphWidgetPage}
        setStatsWidgetPage={this.handleStatsWidgetPage}
      />
    );
  }
}

interface MapDispatchToProps {}
const mapDispatchToProps = (dispatch: any) => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapOrgUnitToProps(dispatch)
});
const mapStateToProps = (state: any, ownProps: any) => ({
  dashboard: getDashboard(state, { dashboard_id: ((ownProps.match?.params as any) || {}).dashboardId }),
  widgets: getWidgetsByDashboardId(state, { dashboard_id: ((ownProps.match?.params as any) || {}).dashboardId }),
  orgUnitListState: dashboardCheckOUListDataState(state),
  selectedWorkspace: getSelectedWorkspace(state),
  OUDataState: getGenericMethodSelector(state, { uri: ORGANIZATION_UNIT_NODE, method: "get" })
});

export default connect(mapStateToProps, mapDispatchToProps)(DashboardViewComponentWrapper);
