import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import Loader from "components/Loader/Loader";
import { tableColumns } from "./table-config";
import { TableRowActions } from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { getDashboardsPage, getBaseUrl } from "constants/routePaths";
import "./dashboard-list.styles.scss";
import { EditCloneModal } from "../shared-resources/components";
import { notification } from "antd";
import { cloneWidgets, filteredDeprecatedWidgets } from "./helpers/helper";
import { debounce, get } from "lodash";
import { v1 as uuid } from "uuid";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import queryString from "query-string";
import { NoDefaultDashboard } from "./components/no-default-dashboard/no-default-dash.component";
import { DEFAULT_DASHBOARD_KEY, NO_DASH } from "./constants/constants";
import { dashboardsSelector } from "reduxConfigs/selectors/dashboardSelector";
import { widgetsSelector } from "reduxConfigs/selectors/widgetSelector";
import { RestWidget } from "../classes/RestDashboards";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { dashboardAccess, ALL_ACCESS_USERS } from "./components/dashboard-settings-modal/helper";
import LocalStoreService from "services/localStoreService";
import { userEntitlementsState } from "reduxConfigs/selectors/entitlements.selector";
import { checkEntitlements } from "custom-hooks/helpers/entitlements.helper";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { DASHBOARD_CREATE_PIVOT_UUID } from "./constants/uuid.constants";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";
import { PageSpinner } from "@harness/uicore";
import { useAppStore } from "contexts/AppStoreContext";
import { useWorkspace } from "custom-hooks/useWorkspace";

export class DashboardsListPage extends React.Component {
  constructor(props) {
    super(props);
    const defaultInParams = queryString.parse(this.props.location.search).default;

    this.state = {
      reload: 0,
      dashboard_deleting: false,
      dashboard_id: undefined,
      clone_id: undefined,
      clone_dashboard_loading: false,
      openEditCloneModel: false,
      clone_dashboard_name: "",
      cloned_dashboard: undefined,
      final_clone_loading: false,
      search_name: undefined,
      name_searching: false,
      name_exist: undefined,
      checkNameListId: undefined,
      loading: defaultInParams !== NO_DASH,
      dashboard_exists: true,
      defaultInParams: defaultInParams,
      selected_ids: [],
      showDeleteModal: false,
      bulk_deleting: false,
      actions_disabled: false,
      clone_dashboard_categories: []
    };

    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.onCloneHandler = this.onCloneHandler.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);
    this.onCancelEditCloneModal = this.onCancelEditCloneModal.bind(this);
    this.onOkEditCloneModal = this.onOkEditCloneModal.bind(this);
    this.checkTemplateName = this.checkTemplateName.bind(this);
    this.clearSelectedIds = this.clearSelectedIds.bind(this);
    this.debounceCheckName = debounce(this.checkTemplateName, 300);
    this.onBulkDelete = this.onBulkDelete.bind(this);
    this.onDeleteSuccess = this.onDeleteSuccess.bind(this);
    this.isEntDashboard = this.isEntDashboard.bind(this);
    this.setPageActions = this.setPageActions.bind(this);
  }

  isEntDashboard() {
    return checkEntitlements(this.props.entitlement, Entitlement.DASHBOARDS);
  }

  componentDidMount() {
    this.setPageActions(this.state.actions_disabled);
  }

  componentWillUnmount() {
    this.props.restapiClear("dashboards", "list", this.state.checkNameListId);
    this.props.restapiClear("dashboards", "delete", "-1");
    this.props.restapiClear("dashboards", "create", "0");
    this.props.restapiClear("dashboards", "bulkDelete", "-1");
  }

  checkTemplateName() {
    const { selectdWorkspace } = this.props;
    if (selectdWorkspace) {
      const filters = {
        filter: {
          partial: {
            name: this.state.search_name
          },
          workspace_id: parseInt(selectdWorkspace.id || "")
        }
      };
      const checkNameListId = uuid();
      this.props.restapiClear("dashboards", "list", this.state.checkNameListId);
      this.props.dashboardsList(filters, checkNameListId);
      this.setState({ name_searching: true, checkNameListId });
    }
  }

  onDeleteSuccess() {
    this.props.restapiClear("dashboards", "delete", this.state.dashboard_id);
    this.props.dashboardDefault(DEFAULT_DASHBOARD_KEY);
    this.setState(prev => {
      return {
        ...prev,
        reload: ++prev.reload,
        selected_ids: this.state.selected_ids.filter(id => id !== this.state.dashboard_id)
      };
    });
  }

  setPageActions(actions_disabled, withBackButton = false) {
    let action_buttons = {};
    const hasAccess = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.DASHBOARD_CREATE)
      : this.props.createAccess;
    if (this.state.defaultInParams !== NO_DASH && hasAccess) {
      action_buttons = {
        create_dashboard: {
          type: "primary",
          label: "New Insights",
          hasClicked: false,
          disabled: actions_disabled,
          tooltip: actions_disabled ? TOOLTIP_ACTION_NOT_ALLOWED : ""
        }
      };
    }
    this.props.setPageSettings(this.props.location.pathname, {
      title: "Insights",
      action_buttons,
      withBackButton,
      bread_crumbs_position: "before"
    });
  }
  componentDidUpdate(prevProps, prevState, snapshot) {
    if (this.props.entitlement !== prevProps.entitlement) {
      const isAllowed = this.isEntDashboard();
      this.setState({ actions_disabled: !isAllowed });
    }

    if (
      prevState.actions_disabled !== this.state.actions_disabled ||
      prevProps.createAccess !== this.props.createAccess
    ) {
      this.setPageActions(this.state.actions_disabled);
    }

    if (this.state.dashboard_deleting) {
      const loading = get(this.props.dashboards, ["delete", this.state.dashboard_id, "loading"], true);
      const error = get(this.props.dashboards, ["delete", this.state.dashboard_id, "error"], true);
      if (!loading) {
        if (!error) {
          const data = get(this.props.dashboards, ["delete", this.state.dashboard_id, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            this.onDeleteSuccess();
          }
          // setTimeout - to introduce delay in the saga changes
          setTimeout(() => {
            this.setState({ loading: true });
          }, 100);
        }
        this.setState(prev => {
          return {
            ...prev,
            dashboard_deleting: false,
            dashboard_id: undefined
          };
        });
      }
    }

    if (this.state.clone_dashboard_loading && !this.state.cloned_dashboard) {
      const { loading, error } = get(this.props.dashboards, ["get", this.state.clone_id], {
        loading: true,
        error: false
      });

      if (!loading && !error) {
        const data = this.props.dashboards.get[this.state.clone_id].data;
        delete data.id;
        const newDashboard = {
          ...data,
          name: this.state.clone_dashboard_name,
          default: false,
          category: this.state.clone_dashboard_categories
        };

        this.props.restapiClear("dashboards", "get", this.state.clone_id);

        this.setState({ cloned_dashboard: newDashboard });
        notification.success({
          message: "Insight cloned successfully."
        });
      }
      if (error) {
        this.setState({
          clone_id: undefined,
          clone_dashboard_loading: false,
          clone_dashboard_name: "",
          cloned_dashboard: undefined,
          clone_widgets_loading: false,
          final_clone_loading: false
        });
        notification.error({
          message: "Failed to clone insight."
        });
      }
    }

    if (this.state.clone_dashboard_loading && this.state.cloned_dashboard && !this.state.final_clone_loading) {
      const _widgets = Object.values(this.props.allWidgets);
      const widgets = _widgets
        .map(w => new RestWidget(w))
        .filter(widget => {
          return widget.dashboard_id === this.state.clone_id && !widget.deleted && RestWidget.isValidWidget(widget);
        })
        .map(w => w.json);
      let updatedWidgets = [];
      if (widgets && widgets.length > 0) {
        const filteredWidgets = filteredDeprecatedWidgets(widgets);
        updatedWidgets = cloneWidgets(filteredWidgets);
      }

      const newDashboard = {
        ...this.state.cloned_dashboard,
        widgets: updatedWidgets
      };

      this.props.dashboardsCreate(newDashboard);

      this.setState({ final_clone_loading: true });
    }

    if (this.state.clone_dashboard_loading && this.state.final_clone_loading) {
      const loading = get(this.props.dashboards, ["create", "0", "loading"], true);
      const error = get(this.props.dashboards, ["create", "0", "error"], true);

      if (!loading && !error) {
        const newDashboard = get(this.props.dashboards, ["create", "0", "data"], {});

        notification.success({
          message: "Insight Cloned successfully"
        });
        const rootOU = this.state?.pivots?.find(elm => elm?.id === this.state?.clone_dashboard_categories?.[0]);
        this.setState(
          {
            clone_id: undefined,
            clone_dashboard_loading: false,
            clone_dashboard_name: "",
            cloned_dashboard: undefined,
            clone_widgets_loading: false,
            final_clone_loading: false
          },
          () => {
            if (rootOU) {
              this.props.history.push(`${getBaseUrl()}/dashboards/${newDashboard?.id}?OU=${rootOU?.root_ou_ref_id}`);
            } else {
              this.props.history.push(`${getBaseUrl()}`);
            }
          }
        );
      }
    }

    if (this.state.search_name && this.state.name_searching) {
      const { loading: _loading, error } = get(this.props.dashboards, ["list", this.state.checkNameListId], {
        loading: true,
        error: true
      });
      if (_loading !== undefined && !_loading && error !== undefined && !error) {
        const data = get(this.props.dashboards, ["list", this.state.checkNameListId, "data", "records"], []);
        this.setState({
          name_exist:
            !!data?.filter(item => item?.name?.toLowerCase() === this.state.search_name?.toLowerCase())?.length ||
            false,
          name_searching: false,
          checkNameListId: undefined
        });
      }
    }

    if (this.state.loading) {
      const { loading, error } = get(this.props.dashboards, ["list", DEFAULT_DASHBOARD_KEY], {
        loading: true,
        error: true
      });
      if (loading !== undefined && loading !== true && error !== undefined && error !== true) {
        const data = get(this.props.dashboards, ["list", DEFAULT_DASHBOARD_KEY, "data", "exists"], true);
        const dashboardCountExceed = checkEntitlements(
          this.props.entitlement,
          Entitlement.DASHBOARDS_COUNT_3,
          EntitlementCheckType.AND
        );
        this.setState({
          loading: false,
          dashboard_exists: data
        });

        this.setPageActions(dashboardCountExceed, true);
        if (!data) {
          this.props.setPageSettings(this.props.location.pathname, {
            title: "Insights",
            action_buttons: {},
            withBackButton: true,
            bread_crumbs_position: "before"
          });
        }
      }
    }

    if (!this.state.loading && this.props.page && Object.keys(this.props.page).length > 0) {
      const page = this.props.page[this.props.location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page.action_buttons.create_dashboard && page.action_buttons.create_dashboard.hasClicked === true) {
          this.props.setPageButtonAction(this.props.location.pathname, "create_dashboard", { hasClicked: false });
          this.props.history.push(`${getDashboardsPage(this.props.match.params)}/create`);
        }
      }
    }

    if (this.state.bulk_deleting) {
      const { loading: _loading, error } = get(this.props.dashboards, ["bulkDelete", "0"]);
      if (!_loading) {
        if (!error) {
          const data = get(this.props.dashboards, ["bulkDelete", "0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            this.setState(state => ({
              bulk_deleting: false,
              reload: state.reload + 1
            }));
          } else {
            this.props.dashboardDefault(DEFAULT_DASHBOARD_KEY);
            this.setState(state => ({
              bulk_deleting: false,
              reload: state.reload + 1,
              selected_ids: []
            }));
          }
          // setTimeout - to introduce delay in the saga changes
          setTimeout(() => {
            this.setState({ loading: true });
          }, 100);
        } else {
          this.setState({ bulk_deleting: false });
        }
      }
    }
    if (!this.state.pivots) {
      const loading = get(this.props.pivots, ["loading"], true);
      const error = get(this.props.pivots, ["error"], true);
      if (!loading && !error) {
        this.setState({ pivots: get(this.props.pivots, ["data", "records"], []) });
      }
    }
  }

  onRemoveHandler(dashboardId) {
    this.setState(
      {
        dashboard_deleting: true,
        dashboard_id: dashboardId
      },
      () => {
        this.props.dashboardsDelete(dashboardId);
      }
    );
  }

  onCloneHandler = dashboardId => {
    this.setState({
      clone_id: dashboardId,
      openEditCloneModel: true
    });
  };

  buildActionOptions(props) {
    const userEmail = new LocalStoreService().getUserEmail();
    const { edit } = dashboardAccess(props);
    let actions = [];
    const entDashboard = this.isEntDashboard();
    let isDeleteAllowed = entDashboard && (userEmail === props.email || ALL_ACCESS_USERS.includes(userEmail));
    const { selectdWorkspace, resourceScope } = this.props;
    const resource = {
      resourceType: ResourceType.SEI_INSIGHTS,
      resourceIdentifier: props.id
    };
    const copyPermission = window.isStandaloneApp
      ? undefined
      : {
          permission: PermissionIdentifier.CREATE_SEI_INSIGHTS,
          resource,
          resourceScope
        };
    const deletePermission = window.isStandaloneApp
      ? undefined
      : {
          permission: PermissionIdentifier.DELETE_SEI_INSIGHTS,
          resource,
          resourceScope
        };
    actions = [
      {
        type: "copy",
        id: props.id,
        description: "Clone",
        onClickEvent: this.onCloneHandler,
        disabled: window.isStandaloneApp ? !edit || !entDashboard || selectdWorkspace?.demo : undefined,
        toolTip: !entDashboard ? TOOLTIP_ACTION_NOT_ALLOWED : "",
        permission: copyPermission
      },
      {
        type: "delete",
        id: props.id,
        onClickEvent: this.onRemoveHandler,
        disabled: window.isStandaloneApp ? !isDeleteAllowed || selectdWorkspace?.demo : undefined,
        toolTip: !entDashboard ? TOOLTIP_ACTION_NOT_ALLOWED : "",
        permission: deletePermission
      }
    ];
    return <TableRowActions actions={actions} />;
  }

  onOkEditCloneModal = (name, categories) => {
    this.props.dashboardsGet(this.state.clone_id);

    this.setState({
      openEditCloneModel: false,
      clone_dashboard_name: name,
      clone_dashboard_categories: categories,
      clone_dashboard_loading: true
    });
  };

  onCancelEditCloneModal = () => {
    this.setState({
      openEditCloneModel: false,
      clone_id: undefined
    });
  };

  clearSelectedIds() {
    this.setState({
      selected_ids: []
    });
  }

  onSelectChange(rowKeys) {
    this.setState({
      selected_ids: rowKeys
    });
  }

  onBulkDelete() {
    this.props.dashboardsBulkDelete(this.state.selected_ids.map(id => parseInt(id)));
    this.setState({
      bulk_deleting: true
    });
  }

  render() {
    if (this.state.delete_loading || this.state.clone_dashboard_loading) {
      return <Loader />;
    }
    const hasEditAccess = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.DASHBOARD_LIST_ROW_SELECTION)
      : this.props.editAccess;
    const rowSelection = hasEditAccess
      ? {
          selectedRowKeys: this.state.selected_ids,
          onChange: this.onSelectChange,
          hideDefaultSelections: false
        }
      : undefined;
    const listActionPermission = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.DASHBOARD_LIST_ACTIONS)
      : true;
    const mappedColumns = listActionPermission
      ? tableColumns.map(column => {
          if (column.key === "id") {
            return {
              ...column,
              render: (item, record, index) => this.buildActionOptions(record)
            };
          }
          return column;
        })
      : tableColumns;

    if (listActionPermission) {
      mappedColumns.push({
        title: "Actions",
        key: "id",
        width: 150,
        render: props => this.buildActionOptions(props)
      });
    }

    if (
      (!this.state.loading && !this.state.dashboard_exists && this.state.defaultInParams === undefined) ||
      this.state.defaultInParams === NO_DASH
    ) {
      return <NoDefaultDashboard history={this.props.history} />;
    }
    const { selectdWorkspace, pageName } = this.props;
    if (!selectdWorkspace?.id) {
      return <PageSpinner />;
    }

    return (
      <>
        <ServerPaginatedTable
          pageName={pageName}
          generalSearchField="name"
          title="Insight List"
          restCall="dashboardsList"
          uri="dashboards"
          reload={this.state.reload}
          columns={mappedColumns}
          // sort={[{ id: "name", desc: true }]}
          {...this.props}
          uuid={"dashboards-list"}
          rowSelection={rowSelection}
          clearSelectedIds={this.clearSelectedIds}
          onBulkDelete={this.onBulkDelete}
          hasDelete={true}
          bulkDeleting={this.state.bulk_deleting}
          moreFilters={{ workspace_id: parseInt(selectdWorkspace.id || "") }}
        />
        <EditCloneModal
          visible={this.state.openEditCloneModel}
          title={"Clone Insight"}
          onOk={this.onOkEditCloneModal}
          onCancel={this.onCancelEditCloneModal}
          searchEvent={event => {
            this.setState(
              {
                search_name: event
              },
              () => this.debounceCheckName()
            );
          }}
          nameExists={this.state.name_exist}
        />
      </>
    );
  }
}

DashboardsListPage.propTypes = {
  moreFilters: PropTypes.object.isRequired,
  partialFilters: PropTypes.object.isRequired,
  hasSearch: PropTypes.bool.isRequired,
  hasFilters: PropTypes.bool.isRequired,
  title: PropTypes.string,
  pageName: PropTypes.string
};

DashboardsListPage.defaultProps = {
  moreFilters: {},
  partialFilters: {},
  hasSearch: true,
  hasFilters: false,
  pageName: "insights"
};

const mapStateToProps = state => ({
  dashboards: dashboardsSelector(state),
  allWidgets: widgetsSelector(state),
  page: getPageSettingsSelector(state),
  entitlement: userEntitlementsState(state),
  selectdWorkspace: getSelectedWorkspace(state),
  pivots: getGenericUUIDSelector(state, { uri: "pivots_list", method: "list", uuid: DASHBOARD_CREATE_PIVOT_UUID })
});

const mapDispatchtoProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch)
});

const DashboardWithConnect = connect(mapStateToProps, mapDispatchtoProps)(DashboardsListPage);

const DashboardsListPageWrapper = props => {
  const [createAccess, editAccess, deleteAccess] = useDashboardPermissions();
  const { accountInfo, selectedProject } = useAppStore();
  const { identifier: accountIdentifier = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
  useWorkspace({
    accountId: accountIdentifier,
    projectIdentifier,
    orgIdentifier
  });
  return (
    <DashboardWithConnect
      {...props}
      createAccess={createAccess}
      editAccess={editAccess}
      deleteAccess={deleteAccess}
      resourceScope={{
        accountIdentifier,
        projectIdentifier,
        orgIdentifier
      }}
    />
  );
};

export default DashboardsListPageWrapper;
