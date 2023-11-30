import { Form, Input, notification, Switch, Tabs, Typography } from "antd";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import Loader from "components/Loader/Loader";
import { DASHBOARD_ROUTES, getBaseUrl } from "constants/routePaths";
import { cloneDeep, debounce, get } from "lodash";
import queryString from "query-string";
import React, { Component } from "react";
import { connect } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
// @ts-ignore
import uuidv1 from "uuid/v1";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { completeDashboardListState } from "reduxConfigs/selectors/restapiSelector";
import LocalStoreService from "services/localStoreService";
import { AntCol, AntModal, AntRow } from "shared-resources/components";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { updateLayoutWithNewApplicationFilters } from "../components/dashboard-application-filters/helper";
import ConfirmationWrapper, { ConfirmationWrapperProps } from "hoc/confirmationWrapper";
import "./dashboard-edit-create-page.scss";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "../../constants/formWarnings";
import { v1 as uuid } from "uuid";
import {
  _dashboardsCreateSelector,
  _dashboardsUpdateSelector,
  dashboardsGetSelector
} from "reduxConfigs/selectors/dashboardSelector";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, DashboardActions } from "dataTracking/analytics.constants";
import { DEFAULT_DASHBOARD_KEY } from "../constants/constants";
import {
  DashboardSettingsModalTitleType,
  DASHBOARD_SETTINGS_PERMISSIONS
} from "dashboard/components/dashboard-settings-modal/helper";
import { ticketcategorizationListSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID } from "dashboard/components/dashboard-settings-modal/constant";
import OrgUnitCategorySelectorComponent from "./OrgUnitCategorySelectorComponent";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { DASHBOARD_CREATE_PIVOT_UUID } from "dashboard/constants/uuid.constants";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { mapWorkspaceToProps } from "reduxConfigs/maps/restapi";
import { SELECTED_OU_HEADER_WORKSPACE } from "reduxConfigs/selectors/workspace/constant";
import DashboardPermissionTab from "dashboard/components/dashboard-settings-modal/DashboardPermissionTab";
import { WebRoutes } from "routes/WebRoutes";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";
import { getIsStandaloneApp } from "helper/helper";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";

const { Text } = Typography;
const { TabPane } = Tabs;

const ls = new LocalStoreService();

interface Props extends ConfirmationWrapperProps, RouteComponentProps {
  dashboardsGet: (id: string) => void;
  dashboardsCreate: (data: any) => void;
  newDashboardUpdate: (id: string, data: any) => void;
  dashboardsList: (filters: any, id: string) => void;
  dashboardDefault: (id: string) => void;
  integrationsGet: (id: string) => void;
  setPageSettings: (path: string, settings: any) => void;
  setPageButtonAction: (path: string, btnType: string, attributes: any) => void;
  clearPageSettings: (path: string) => void;
  restapiClear: (uri: string, method: string, id?: string) => void;
  workspaceClear: (id: string) => void;
  ticketCategorizationSchemesList: (filters: any, id: string) => void;
  rest_api?: any;
  dashboard?: any;
  dashboard_create?: any;
  dashboard_update?: any;
  dashboard_list?: any;
  page?: any;
  location: any;
  history: any;
  ticket_categorization_list: any;
  pivots?: any;
  selectedWorkspace?: WorkspaceModel;
}

interface State {
  dashboard_loading: boolean;
  dashboard_updating: boolean;
  dashboard_creating: boolean;
  dashboard_id?: string;
  dashboard: RestDashboard | undefined;
  create_modal: boolean;
  dashboard_query: any;
  setHeaderName: boolean;
  application_filters_modal: boolean;
  updatedWidgetsMap: { [x: string]: number };
  dashboardTitle: string | undefined;
  nameFieldBlur: boolean;
  nameExists: boolean;
  name_searching: boolean;
  selected_ou_category?: string;
  checkNameListId: any;
  defaultInParams: boolean;
  metadata: any;
  category: any;
  ticket_categorization_list_state: any;
  ticket_categorization_list_loading: boolean;
  pivots?: any;
  permissions?: any;
}

class DashboardEditCreatePageComponent extends Component<Props & { dashboardCreateAccess: boolean }, State> {
  allowedDashboardNameLength = 100;

  constructor(props: any) {
    super(props);

    const dashboardId = queryString.parse(this.props.location.search).id;
    const selectedOUCategoryId = queryString.parse(this.props.location.search).ou_category_id;
    const defaultInParams = queryString.parse(this.props.location.search).default;

    this.state = {
      dashboard_loading: dashboardId !== undefined,
      dashboard_id: !!dashboardId ? (dashboardId as string) : undefined,
      dashboard: undefined,
      selected_ou_category: selectedOUCategoryId as string,
      dashboard_creating: false,
      dashboard_updating: false,
      create_modal: !dashboardId,
      dashboard_query: {},
      setHeaderName: false,
      application_filters_modal: false,
      updatedWidgetsMap: {},
      dashboardTitle: undefined,
      nameFieldBlur: false,
      nameExists: false,
      name_searching: false,
      category: selectedOUCategoryId ? [selectedOUCategoryId] : [],
      checkNameListId: undefined,
      defaultInParams: defaultInParams === "true",
      metadata: {
        dashboard_time_range: true,
        rbac: { owner: ls.getUserEmail(), dashboardPermission: "admin", users: {} }
      },
      ticket_categorization_list_state: [],
      ticket_categorization_list_loading: true
    };

    this.onWidgetCreate = this.onWidgetCreate.bind(this);
    this.onLayoutChange = this.onLayoutChange.bind(this);
    this.onWidgetRemove = this.onWidgetRemove.bind(this);
    this.onFilterChange = this.onFilterChange.bind(this);
    this.onWidgetClone = this.onWidgetClone.bind(this);
    this.save = this.save.bind(this);
    this.setDashboard = this.setDashboard.bind(this);
    this.onOkGlobalApplicationModal = this.onOkGlobalApplicationModal.bind(this);
    this.onWidgetDataChange = this.onWidgetDataChange.bind(this);
    this.getValidateStatus = this.getValidateStatus.bind(this);
    this.getError = this.getError.bind(this);
    this.handleSetMetadataState = this.handleSetMetadataState.bind(this);
    this.checkTemplateName = this.checkTemplateName.bind(this);
    // @ts-ignore
    this.debounceCheckName = debounce(this.checkTemplateName, 300);
  }

  isNameLong(): boolean {
    return (this.state.dashboardTitle || "").trim().length > this.allowedDashboardNameLength;
  }

  isNameEmpty(): boolean {
    return this.state.dashboardTitle ? this.state.dashboardTitle?.trim().length === 0 : true;
  }

  checkTemplateName() {
    const filters = {
      filter: {
        partial: {
          name: this.state.dashboardTitle?.trim()
        },
        workspace_id: !!this.props?.selectedWorkspace?.id ? parseInt(this.props?.selectedWorkspace?.id) : ""
      }
    };
    const checkNameListId = uuid();
    this.props.dashboardsList(filters, checkNameListId);
    this.setState({ name_searching: true, checkNameListId });
  }

  getValidateStatus() {
    if (!this.state.nameFieldBlur) {
      return "";
    } else if (
      this.state.nameFieldBlur &&
      (this.state.dashboardTitle || "").trim().length > 0 &&
      !this.state.nameExists
    ) {
      return "success";
    } else return "error";
  }

  getError() {
    if (this.state.nameExists === true) {
      return NAME_EXISTS_ERROR;
    } else return REQUIRED_FIELD;
  }

  handleSetMetadataState(key: string, value: any) {
    this.setState(state => {
      return {
        ...(state || {}),
        metadata: {
          ...(state?.metadata || {}),
          [key]: value
        }
      };
    });
  }

  get createModal() {
    let _default: any;
    let _publicdefault: any;

    let name: any;
    let isDefault = this.state.defaultInParams || false;
    let isPublic: any;
    let query = this.state.dashboard_query;

    if (this.state.dashboard) {
      name = this.state.dashboard!.name;
      isDefault = this.state.dashboard!.default || false;
      isPublic = [DASHBOARD_SETTINGS_PERMISSIONS.LIMITED, DASHBOARD_SETTINGS_PERMISSIONS.PUBLIC]?.includes(
        this.state.metadata?.rbac?.dashboardPermission
      );
      if (this.state.dashboardTitle === undefined) {
        this.setState({ dashboardTitle: name });
      }
    }

    const basicInfoView = (
      <AntRow gutter={[16, 16]}>
        <AntCol span={24}>
          <Form layout="vertical">
            <Form.Item
              label="Name"
              required
              validateStatus={this.getValidateStatus()}
              hasFeedback={true}
              help={this.getValidateStatus() === "error" && this.getError()}
              colon={false}>
              <Input
                name={"Name"}
                onChange={e =>
                  this.setState({ dashboardTitle: e.target?.value }, () => {
                    // @ts-ignore
                    this.debounceCheckName();
                  })
                }
                defaultValue={name}
                value={this.state.dashboardTitle}
                onFocus={() => {
                  this.setState({ nameFieldBlur: true });
                }}
              />
              {this.isNameLong() && (
                <span className="error-msg">Maximum character limit ({this.allowedDashboardNameLength}) reached.</span>
              )}
            </Form.Item>

            <Form.Item label={DashboardSettingsModalTitleType.PARAMETERS} colon={false}>
              <div className="form-content">
                <Typography.Text className="form-content__text">Insights Time Range</Typography.Text>
                <Switch
                  checked={isDashboardTimerangeEnabled(this.state.metadata)}
                  onChange={value => {
                    this.setState(state => {
                      return {
                        ...state,
                        metadata: {
                          ...state.metadata,
                          dashboard_time_range: value
                        }
                      };
                    });
                  }}
                  className="form-content__switch"
                />
              </div>
              <div className="form-content">
                <Typography.Text className="form-content__text">Effort Investment Profile</Typography.Text>
                <Switch
                  onChange={value => {
                    this.setState(state => {
                      return {
                        ...state,
                        metadata: {
                          ...state.metadata,
                          effort_investment_profile: value
                        }
                      };
                    });
                  }}
                  className="form-content__switch"
                />
              </div>
              <div className="form-content">
                <Typography.Text className="form-content__text">Effort Investment Units</Typography.Text>
                <Switch
                  onChange={value => {
                    this.setState(state => {
                      return {
                        ...state,
                        metadata: {
                          ...state.metadata,
                          effort_investment_unit: value
                        }
                      };
                    });
                  }}
                  className="form-content__switch"
                />
              </div>
              <div className="form-content">
                <Typography.Text className="form-content__text">Integration Monitoring</Typography.Text>
                <Switch
                  onChange={value => {
                    this.setState(state => {
                      return {
                        ...state,
                        metadata: {
                          ...state.metadata,
                          integration_monitoring: value
                        }
                      };
                    });
                  }}
                  className="form-content__switch"
                />
              </div>
            </Form.Item>
            <OrgUnitCategorySelectorComponent
              ouCategories={get(this.state, ["category"])}
              handleOUCategoryChange={value =>
                this.setState(state => {
                  return {
                    ...state,
                    category: value
                  };
                })
              }
            />
          </Form>
        </AntCol>
      </AntRow>
    );

    const invalidName = this.isNameEmpty() || this.isNameLong() || this.state.nameExists;
    const invalidCategory = !get(this.state, ["category"], []).length;
    const disabled = invalidName || getIsStandaloneApp() ? invalidCategory : !this.props.dashboardCreateAccess;

    return (
      <AntModal
        width={600}
        title={
          this.state.dashboard
            ? DashboardSettingsModalTitleType.DASHBOARD_SETTINGS
            : DashboardSettingsModalTitleType.CREATE_DASHBOARD
        }
        mask={true}
        maskClosable={false}
        visible={this.state.create_modal}
        onOk={() =>
          this.onDashboardCreate(
            this.state.dashboardTitle || name,
            false,
            [DASHBOARD_SETTINGS_PERMISSIONS.LIMITED, DASHBOARD_SETTINGS_PERMISSIONS.PUBLIC]?.includes(
              this.state.metadata?.rbac?.dashboardPermission
            )
          )
        }
        okButtonProps={{
          disabled
        }}
        // TODO handle redirect to list page if there is no existing dashboard
        onCancel={() =>
          this.setState(
            {
              create_modal: false,
              dashboard_query: this.state.dashboard ? this.state.dashboard!.query : {}
            },
            () => {
              if (!this.state.dashboard) {
                this.props.history.goBack();
              }
            }
          )
        }
        okText={this.state.dashboard_creating ? "Creating..." : "Create"}
        closable={true}>
        {getIsStandaloneApp() ? (
          <Tabs defaultActiveKey="1">
            <TabPane tab="Basic" key="1">
              {basicInfoView}
            </TabPane>
            <TabPane tab="Permissions" key="2">
              <DashboardPermissionTab
                email={ls.getUserEmail()}
                permissions={this.state?.metadata}
                setPermissions={(key: any, val: any) => {
                  this.handleSetMetadataState(key, val.rbac);
                }}
              />
            </TabPane>
          </Tabs>
        ) : (
          basicInfoView
        )}
      </AntModal>
    );
  }

  static getDerivedStateFromProps(props: Props, state: State) {
    if (state.dashboard_loading || state.dashboard_creating) {
      const { loading, error, data: dashboard } = props.dashboard;

      if (loading !== undefined && loading === false && error !== undefined && error === false) {
        // setting header
        props.setPageSettings(props.location.pathname, {
          title: dashboard.name ? dashboard.name : "Dashboard",
          action_buttons: {
            secondary: {
              type: "secondary",
              label: "Settings",
              icon: "setting",
              hasClicked: false
            },
            filters: {
              type: "secondary",
              label: "Global Filters",
              hasClicked: false
            },
            primary: {
              type: "primary",
              label: "Save",
              icon: "save",
              hasClicked: false
            },
            cancel: {
              type: "cancel",
              label: "Cancel",
              hasClicked: false
            }
          }
        });

        return {
          ...state,
          dashboard_loading: false,
          dashboard: new RestDashboard(dashboard),
          dashboard_query: { ...dashboard!.query },
          setHeaderName: true
        };
      }

      // It's loaded now. Handle Errors.
      if (loading !== undefined && loading === false && error) {
        const errorResponse = props.dashboard.data;
        let errorMessage = "Failed to load insight. Please try again.";

        if (!errorResponse) {
          notification.error({
            message: errorMessage
          });
        } else {
          const { status, message } = errorResponse;
          if (status === 404) {
            // Handle not found.
            notification.error({
              message: "Requested Insight doesn't exist"
            });
            props.history.push(`${getBaseUrl(props.match.params)}${DASHBOARD_ROUTES._ROOT}`);
          } else {
            notification.error({
              message: message ?? errorMessage
            });
          }
        }

        return {
          dashboard_loading: false
        };
      }
    }

    if (state.dashboard_updating) {
      let dashboardId: any = "0";
      if (props.location && props.location.search) {
        dashboardId = queryString.parse(props.location.search).id || "0";
      }
      const { loading, error } = get(props.dashboard_update, [dashboardId], { loading: true, error: true }); // This can be optimized more.
      // TODO: Handle update error...
      // this is not the props for update to complete, so i need to do something else
      if (loading !== undefined && error !== undefined && loading === false && error === false) {
        props.restapiClear("dashboards", "update", state.dashboard_id);
        props.dashboardDefault(DEFAULT_DASHBOARD_KEY);
        props.onSaveActionComplete && props.onSaveActionComplete(false);
        notification.info({
          message: "Dashboard updated successfully"
        });
        props.history.push(`${getBaseUrl(props.match.params)}${DASHBOARD_ROUTES._ROOT}/${state.dashboard_id}`);
        return {
          ...state,
          dashboard_updating: false,
          dashboard_loading: true
        };
      } else {
        if (loading !== undefined && error !== undefined && loading === false && error === true) {
          props.onSaveActionComplete && props.onSaveActionComplete(true);
          props.setPageButtonAction(props.location.pathname, "primary", {
            hasClicked: false,
            disabled: false,
            showProgress: false
          });
          notification.error({
            message: "Dashboard update failed"
          });
          return {
            ...state,
            dashboard_updating: false
          };
        }
      }
    }

    if (state.dashboard_creating) {
      const { loading, error } = props.dashboard_create;

      if (!loading && !error && props.dashboard_create.data !== undefined) {
        const newDashboard = props.dashboard_create.data;
        props.dashboardDefault(DEFAULT_DASHBOARD_KEY);
        const newDashboardId = newDashboard.id;
        props.restapiClear("dashboards", "create", "0");
        props.onSaveActionComplete && props.onSaveActionComplete(false);
        const rootOU = state?.pivots?.find((elm: any) => elm?.id === state?.category?.[0]);
        // Redirect to preview URL
        if (rootOU) {
          props.history.push(
            `${getBaseUrl(props.match.params)}/dashboards/${newDashboardId}?OU=${rootOU?.root_ou_ref_id}&workspace_id=${
              props?.selectedWorkspace?.id
            }`
          );
        } else {
          props.history.push(`${getBaseUrl()}`);
        }

        return {
          ...state,
          dashboard_id: newDashboardId,
          dashboard_creating: false,
          dashboard_loading: true
        };
      } else {
        if (loading !== undefined && error !== undefined && loading === false && error === true) {
          props.onSaveActionComplete && props.onSaveActionComplete(true);
        }
      }
    }

    if (
      queryString.parse(props.location.search).id &&
      queryString.parse(props.location.search).id !== state.dashboard_id
    ) {
      if (state.setHeaderName) {
        return {
          ...state,
          setHeaderName: false
        };
      }
    }

    if (
      !state.dashboard_loading &&
      !state.dashboard_creating &&
      !state.dashboard_updating &&
      state.dashboard &&
      !state.setHeaderName
    ) {
      props.setPageSettings(props.location.pathname, {
        title: state.dashboard.name ? state.dashboard.name : "Dashboard",
        action_buttons: {
          secondary: {
            type: "secondary",
            label: "Settings",
            icon: "setting",
            hasClicked: false
          },
          filters: {
            type: "secondary",
            label: "Global Filters",
            hasClicked: false
          },
          primary: {
            type: "primary",
            label: "Save",
            icon: "save",
            progressLabel: "Saving...",
            hasClicked: false
          },
          cancel: {
            type: "cancel",
            label: "Cancel",
            hasClicked: false
          }
        }
      });

      return {
        ...state,
        setHeaderName: true
      };
    }

    if (!state.dashboard_loading && state.dashboard && props.page && Object.keys(props.page).length > 0) {
      const page = props.page[props.location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page.action_buttons.primary && page.action_buttons.primary.hasClicked === true) {
          // LEV-2065 User is saving, meaning
          // the Edit Dashboard page won't be dirty anymore.
          props.setDirty(false);
          if (state.dashboard_id) {
            props.restapiClear("dashboards", "get", state.dashboard_id);
            props.newDashboardUpdate(state.dashboard_id!, state.dashboard!.json);
            props.setPageButtonAction(props.location.pathname, "primary", {
              hasClicked: false,
              disabled: true,
              showProgress: true
            });

            return {
              ...state,
              dashboard_updating: true
            };
          } else {
            props.dashboardsCreate(state.dashboard!.json);
            props.setPageButtonAction(props.location.pathname, "primary", {
              hasClicked: false,
              disabled: true,
              showProgress: true
            });

            return {
              dashboard_creating: true
            };
          }
        }

        if (page.action_buttons.secondary && page.action_buttons.secondary.hasClicked === true) {
          props.setPageButtonAction(props.location.pathname, "secondary", { hasClicked: false });
          return {
            ...state,
            create_modal: true
          };
        }

        if (page.action_buttons.filters && page.action_buttons.filters.hasClicked === true) {
          props.setPageButtonAction(props.location.pathname, "filters", { hasClicked: false });
          return {
            ...state,
            application_filters_modal: true
          };
        }

        if (page.action_buttons.cancel && page.action_buttons.cancel.hasClicked === true) {
          props.setPageButtonAction(props.location.pathname, "cancel", { hasClicked: false });
          if (state.dashboard_id) {
            const path = `${getBaseUrl(props.match.params)}${DASHBOARD_ROUTES.LIST}/${state.dashboard_id}`;
            props.setHardRedirect && props.setHardRedirect(true);
            props.history.push(path);
          } else {
            props.history.push(`${getBaseUrl(props.match.params)}${DASHBOARD_ROUTES.LIST}`);
          }

          return state;
        }
      }
      return state;
    }

    if (state.dashboardTitle && state.name_searching) {
      const { loading: _loading, error } = get(props.dashboard_list, [state.checkNameListId], {
        loading: true,
        error: true
      });
      if (_loading !== undefined && !_loading && error !== undefined && !error) {
        const data = get(props.dashboard_list, [state.checkNameListId, "data", "records"], []);
        return {
          nameExists:
            !!data?.filter((item: any) => item?.name?.toLowerCase() === state.dashboardTitle?.trim().toLowerCase())
              ?.length || false,
          name_searching: false,
          checkNameListId: undefined
        };
      }
    }

    if (state.ticket_categorization_list_loading) {
      const loading = get(props.ticket_categorization_list, ["loading"], true);
      const error = get(props.ticket_categorization_list, ["error"], true);
      if (!loading && !error) {
        return {
          ticket_categorization_list_state: get(props.ticket_categorization_list, ["data", "records"], []),
          ticket_categorization_list_loading: false
        };
      }
    }
    if (!state.pivots) {
      const loading = get(props.pivots, ["loading"], true);
      const error = get(props.pivots, ["error"], true);
      if (!loading && !error) {
        return {
          ...state,
          pivots: get(props.pivots, ["data", "records"], [])
        };
      }
    }
    return null;
  }

  componentDidMount() {
    const { bindSaveAction, ticketCategorizationSchemesList } = this.props;
    bindSaveAction && bindSaveAction(this.save);
    ticketCategorizationSchemesList(
      { filter: { default: true }, page_size: 1000 },
      EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID
    );
    if (this.state.dashboard_id) {
      const dashboard = this.props.dashboard.data;
      if (dashboard && dashboard.id === this.state.dashboard_id) {
        this.setState({
          dashboard_loading: false,
          dashboard: new RestDashboard(dashboard),
          dashboard_query: { ...dashboard!.query }
        });
      } else {
        this.setState({ dashboard_loading: true }, () => {
          this.props.dashboardsGet(this.state.dashboard_id as string);
        });
      }
    } else {
      // @ts-ignore
      this.props.history.push(WebRoutes.dashboard.create(this.props.match.params, this.state.selected_ou_category));
    }
  }

  componentWillUnmount() {
    if (this.state.dashboard_id) {
      this.props.restapiClear("dashboards", "get", "-1");
    }

    this.props.restapiClear("dashboards", "update", "-1");
    this.props.restapiClear("dashboards", "delete", "-1");
    this.props.restapiClear("products", "get", "-1");
    this.props.restapiClear("mappings", "list", "-1");
    this.props.restapiClear("integrations", "get", "-1");
    this.props.clearPageSettings(this.props.location.pathname);
    this.props.workspaceClear(SELECTED_OU_HEADER_WORKSPACE);
  }

  onWidgetCreate(uuid: string, widgetType: string, name: string = "", hidden: boolean = false) {
    const dashboard = this.state.dashboard;
    let widgets = dashboard!.widgets;
    const newWidget = new RestWidget({
      id: uuid,
      name: name,
      type: undefined,
      metadata: {
        width: "half",
        order: dashboard!.widgets.length + 1,
        widget_type: widgetType,
        children: [],
        hidden: hidden
      },
      query: {}
    });
    // @ts-ignore
    widgets.push(newWidget);
    dashboard!.widgets = widgets;
    this.setState({ dashboard: dashboard });
    return newWidget;
  }

  onWidgetClone(clonedWidgetId: string, cloningWidgetId: string) {
    const dashboard = this.state.dashboard;
    let widgets = dashboard!.widgets;
    const widgetData = widgets.find((widget: any) => widget.id === cloningWidgetId);
    const clonedWidget = cloneDeep(widgetData);
    clonedWidget.id = clonedWidgetId;
    clonedWidget.name = "Copy of " + widgetData.name;
    clonedWidget.children = [];
    widgets.push(clonedWidget);
    widgetData.children.forEach((widgetId: string) => {
      const cloningChildWidget = widgets.find((widget: any) => widget.id === widgetId);
      if (!!cloningChildWidget) {
        const clonedChildWidget = cloneDeep(cloningChildWidget);
        clonedChildWidget.id = uuidv1();
        clonedChildWidget.name = "Copy of " + cloningChildWidget.name;
        clonedWidget.children.push(clonedChildWidget.id);
        widgets.push(clonedChildWidget);
      }
    });
    dashboard!.widgets = widgets;
    this.setState({ dashboard: dashboard });
  }

  onWidgetRemove(id: string) {
    // LEV-2065
    this.props.setDirty(true);

    const dashboard = this.state.dashboard;
    let widgets = dashboard!.widgets;
    dashboard!.widgets = widgets.filter((widget: any) => widget.id !== id);
    this.setState({ dashboard: dashboard });
  }

  setDashboard = () => {
    const { page, location, setPageSettings } = this.props;
    let _page = page[location.pathname];
    if (_page && _page.hasOwnProperty("title")) {
      setPageSettings(location.pathname, {
        ..._page,
        title: this.state.dashboard!.name || "Dashboard"
      });
    }
  };

  onDashboardCreate(name: string, isDefault: boolean, isPublic: boolean) {
    // LEV-2065
    // Either new dashboard is created (on UI only), but hasn't been saved,
    // or existing dashboard's settings have been changed.

    let metadata = this.state.metadata;

    if (isDashboardTimerangeEnabled(this.state.metadata)) {
      metadata.dashboard_time_range_filter = "last_30_days";
    }

    if (this.state.metadata.effort_investment_profile) {
      const defaultValue = this.state.ticket_categorization_list_state.find((item: any) => item.default_scheme);
      metadata.effort_investment_profile_filter = defaultValue.id;
    }

    if (this.state.metadata.effort_investment_unit) {
      metadata.effort_investment_unit_filter = "%_of_engineers";
    }

    if (!this.state.dashboard) {
      const dashboard = new RestDashboard({
        name: name?.trim(),
        query: this.state.dashboard_query,
        owner_id: ls.getUserId(), // Saving id of logged in user
        widgets: [],
        default: isDefault,
        public: isPublic,
        metadata: metadata,
        category: this.state.category
      });
      this.setState(
        {
          dashboard_creating: true
        },
        () => this.props.dashboardsCreate(dashboard.json)
      );
      const gaCategory = AnalyticsCategoryType.ORG_UNITS;
      emitEvent(gaCategory, DashboardActions.CREATE_NEW);
    } else {
      this.props.setDirty(true);

      const dashboard = this.state.dashboard;
      dashboard!.name = name?.trim();
      dashboard!.default = isDefault;
      dashboard!.public = isPublic;
      dashboard!.query = this.state.dashboard_query;

      let updatedWidgetsMap = {};
      const widgetIds = (dashboard?.widgets || []).map((widget: any) => widget.id);
      updatedWidgetsMap = widgetIds.reduce((acc: any, id: string) => {
        return {
          ...acc,
          [id]: get(this.state.updatedWidgetsMap, [id], 0) + 1
        };
      }, {});

      updatedWidgetsMap = {
        ...this.state.updatedWidgetsMap,
        ...updatedWidgetsMap
      };

      this.setState({ dashboard: dashboard, updatedWidgetsMap, create_modal: false }, () => this.setDashboard());
    }
  }

  onLayoutChange(widgets: any) {
    const dashboard = this.state.dashboard;
    let currentWidgets = dashboard!.widgets;
    currentWidgets.forEach((w: any) => {
      const changedWidget = widgets.find((wid: any) => wid.id === w.id);
      if (changedWidget) {
        w.order = changedWidget.order;
        w.width = changedWidget.width;
      }
    });
    dashboard!.widgets = currentWidgets;
    this.setState({ dashboard: dashboard });
  }

  onFilterChange(filters: any) {
    this.setState({
      dashboard_query: { ...filters }
    });
    //const dashboard = this.state.dashboard;
    //dashboard!.query = filters;
    //this.setState({ dashboard: dashboard });
  }

  save(data: any) {
    if (this.state.dashboard_id) {
      // update the dashboard
      this.props.restapiClear("dashboards", "get", this.state.dashboard_id);
      this.setState(
        {
          dashboard_updating: true
        },
        () => this.props.newDashboardUpdate(this.state.dashboard_id!, this.state.dashboard!.json)
      );
    } else {
      // create the dashboard
      this.setState(
        {
          dashboard_creating: true
        },
        () => this.props.dashboardsCreate(this.state.dashboard!.json)
      );
    }
  }

  onWidgetDataChange = (newData: any) => {
    const dashboard = this.state.dashboard;
    const newWidgets = newData.filter((widget: any) => widget.type !== undefined);
    const prev = newWidgets.map((data: any) => data.id);
    dashboard!.widgets = dashboard!.widgets.filter(
      (widget: any) => widget.type !== undefined && !prev.includes(widget.id)
    );
    dashboard!.widgets = [...dashboard!.widgets, ...newWidgets];
    this.setState({ dashboard: dashboard });
  };

  get integration_ids() {
    return get(this.state.dashboard, ["query", "integration_ids"], []);
  }

  onOkGlobalApplicationModal = (filters: any, update: boolean = false) => {
    const dashboard = this.state.dashboard;
    dashboard!.global_filters = filters;
    let updatedWidgetsMap = {};
    if (update) {
      const [updatedWidgets, updatedIds] = updateLayoutWithNewApplicationFilters(
        cloneDeep(dashboard!.widgets || []),
        filters
      );
      dashboard!.widgets = updatedWidgets;
      updatedWidgetsMap = updatedIds.reduce((acc, id) => {
        return {
          ...acc,
          [id]: get(this.state.updatedWidgetsMap, [id], 0) + 1
        };
      }, {});
    }

    updatedWidgetsMap = {
      ...this.state.updatedWidgetsMap,
      ...updatedWidgetsMap
    };

    // LEV-2065
    // Global filters can be saved on dashboards.
    this.props.setDirty(true);

    this.setState({ application_filters_modal: false, dashboard: dashboard, updatedWidgetsMap });
  };

  render() {
    if (this.state.dashboard_loading) {
      return <Loader />;
    }

    return (
      <>
        {this.state.create_modal && this.createModal}
        {/*{this.state.application_filters_modal && (*/}
        {/*  <DashboardApplicationFilters*/}
        {/*    filters={this.state.dashboard!.global_filters}*/}
        {/*    integrationIds={this.integration_ids}*/}
        {/*    onCancel={() => this.setState({ application_filters_modal: false })}*/}
        {/*    onOk={this.onOkGlobalApplicationModal}*/}
        {/*    visible={this.state.application_filters_modal}*/}
        {/*  />*/}
        {/*)}*/}
      </>
    );
  }
}

const mapStateToProps = (state: any, ownProps: any) => {
  let dashboardId: any = "0";
  if (ownProps.location && ownProps.location.search) {
    dashboardId = queryString.parse(ownProps.location.search).id || "0";
  }
  // @ts-ignore
  return {
    dashboard: dashboardsGetSelector(state, { dashboard_id: dashboardId }),
    dashboard_create: _dashboardsCreateSelector(state),
    dashboard_update: _dashboardsUpdateSelector(state),
    dashboard_list: completeDashboardListState(state),
    page: getPageSettingsSelector(state),
    ticket_categorization_list: ticketcategorizationListSelector(state),
    pivots: getGenericUUIDSelector(state, { uri: "pivots_list", method: "list", uuid: DASHBOARD_CREATE_PIVOT_UUID }),
    selectedWorkspace: getSelectedWorkspace(state)
  };
};

const mapDispatchToProps = (dispatch: any) => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch),
  ...mapWorkspaceToProps(dispatch)
});

const DashboardEditCreateWrapper = (props: Props) => {
  const [dashboardCreateAccess] = useDashboardPermissions();
  return <DashboardEditCreatePageComponent {...props} dashboardCreateAccess={dashboardCreateAccess} />;
};

// @ts-ignore
export default connect(mapStateToProps, mapDispatchToProps)(ConfirmationWrapper(DashboardEditCreateWrapper as any));
