import React from "react";
import { connect } from "react-redux";
import { Link as RouterLink } from "react-router-dom";
import { AntButton, AntSelect, AntTag, AntText, AntTitle, AntTooltip } from "shared-resources/components";
import { get } from "lodash";
import Icon from "antd/lib/icon";
import cx from "classnames";
import { Breadcrumb, Button, Divider, Dropdown, Layout, Menu, Select, Switch } from "antd";
import * as PropTypes from "prop-types";
import { toggleFullscreenModalAction } from "reduxConfigs/actions/ui.actions";
import { getHeaderBreadcrumbLabel, getHeaderBreadcrumbTo, setMenuHeaderActions } from "./helper";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import "./header.style.scss";
import HeaderNameMenuAction from "./HeaderNameMenuAction";
import HeaderBackButton from "./HeaderBackButton";
import BreadCrumbComponent from "./BreadcrumbComponent";
import { isDashboardViewPage, isHarnessHomePage, isScoreDashboard } from "utils/dashboardUtils";
import { isIntegrationMapping } from "utils/routeUtils";
import { truncateAndEllipsis } from "utils/stringUtils";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";

const { Header } = Layout;
const { Option } = Select;
const Crumb = props => <RouterLink {...props} component={RouterLink} />;

export class HeaderContainer extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      header_title: undefined,
      header_description: undefined
    };

    this.onActionClickHandler = this.onActionClickHandler.bind(this);
  }

  onActionClickHandler(currentPage) {
    return () => {
      this.props.toggleFullscreenModal(currentPage, true);
    };
  }

  componentWillUpdate(nextProps, nextState, nextContext) {
    if (Object.keys(nextProps.page_settings).length > 0) {
      const page = nextProps.page_settings[nextProps.location.pathname];
      if (page && page.hasOwnProperty("title")) {
        this.setState({ header_title: page.title });
      } else {
        this.setState({ header_title: undefined });
      }

      if (page && page.hasOwnProperty("description")) {
        this.setState({ header_description: page.description });
      } else {
        this.setState({ header_description: undefined });
      }
    }
  }

  get selectButtons() {
    const { page_settings, setPageSwitchAction, location } = this.props;
    const page = page_settings[location.pathname];
    if (!page || !page.hasOwnProperty("select_buttons")) {
      return null;
    }
    return Object.keys(page.select_buttons).map((btnType, index) => {
      const btn = page.select_buttons[btnType];
      const handleChange = () => {
        if (btn.onChangeHandler) {
          btn.onChangeHandler();
          return;
        }
        setPageSwitchAction(location.pathname, btnType, { hasToggled: true, checked: !btn.checked });
      };
      return (
        <div className="mr-10" key={index}>
          <AntText className="mr-10">{btn.label}</AntText>
          <Switch checked={btn.checked} onChange={handleChange} />
        </div>
      );
    });
  }

  get dropDownButtons() {
    const { page_settings, setPageDropDownAction, location } = this.props;
    const page = page_settings[location.pathname];
    if (!page || !page.hasOwnProperty("dropdown_buttons")) {
      return null;
    }
    return Object.keys(page.dropdown_buttons).map((btnType, index) => {
      const btn = page.dropdown_buttons[btnType];
      const menu = (
        <Menu
          onClick={e => {
            const selectedOption = btn.options.find(option => option.key === e.key);
            setPageDropDownAction(location.pathname, btnType, {
              hasClicked: true,
              value: e.key,
              selected_option: selectedOption
            });
          }}>
          {(btn.options || []).map(item => (
            <Menu.Item key={item.key}>
              {item.icon && <Icon type={item.icon} />}
              {item.label}
            </Menu.Item>
          ))}
        </Menu>
      );
      let btnLabel = btn.label || "";
      if (btn.selected_option && btn.selected_option.label) {
        btnLabel = btn.selected_option.label;
      }
      return (
        <div key={index} style={{ marginRight: "10px" }}>
          <Dropdown overlay={menu}>
            <Button>
              {btnLabel} <Icon type="down" />
            </Button>
          </Dropdown>
        </div>
      );
    });
  }

  get workspaceDropDownButtons() {
    const { page_settings, setPageSelectDropDownAction, location } = this.props;
    const page = page_settings[location.pathname];
    if (!page || !page.hasOwnProperty("select_dropdown")) {
      return null;
    }

    const renderLabel = label => {
      if (label) {
        const MAX_LABEL_LENGTH = 15;
        if (label.length > MAX_LABEL_LENGTH) {
          return <AntTooltip title={label}>{truncateAndEllipsis(label, MAX_LABEL_LENGTH)}</AntTooltip>;
        }
        return label;
      }
      return "";
    };

    return Object.keys(page.select_dropdown).map((btnType, index) => {
      const btn = page?.select_dropdown[btnType];
      const label = btn?.label;
      return (
        <div className="workspace-select-div">
          <AntText className="workspace-select-div__text">{label}</AntText>
          <AntSelect
            className="workspace-select-div__select"
            dropdownClassName="header-select-dropdown"
            value={btn?.value || btn?.selected_option?.value}
            onChange={e => {
              const selectedOption = (btn?.options ?? []).find(option => option.value === e);
              setPageSelectDropDownAction(location.pathname, btnType, {
                hasClicked: true,
                value: e,
                selected_option: selectedOption
              });
            }}>
            {(btn?.options || []).map(option => (
              <Option
                value={option?.value}
                className="select-dropdown-option"
                label={option?.label}
                key={option?.label}>
                <AntText>{renderLabel(option?.label)}</AntText>
              </Option>
            ))}
          </AntSelect>
        </div>
      );
    });
  }

  isActionButtonAllowed(btn) {
    const { location, selectedWorkspace } = this.props;

    if (btn?.disabled) return false;

    // All users should access to propelo account users
    if (btn?.actionId === "users") return true;

    // All actions under demo workspace is disabled
    if (selectedWorkspace?.demo) return false;

    return true;
  }

  get actionButtons() {
    const { page_settings, setPageButtonAction, location } = this.props;
    const page = page_settings[location.pathname];
    if (!page || !page.hasOwnProperty("action_buttons")) {
      return null;
    }
    return Object.keys(page.action_buttons)
      .filter(btnType => page.action_buttons[btnType].type !== "hidden")
      .map((btnType, index) => {
        const btn = page.action_buttons[btnType];
        const handleClick = () => {
          if (btn.buttonHandler) {
            btn.buttonHandler();
            return;
          }
          setPageButtonAction(location.pathname, btnType, { hasClicked: true });
        };
        return (
          <div key={index} style={{ marginRight: "10px" }}>
            <AntTooltip title={btn.tooltip}>
            <span>
              <AntButton
                type={btn.type || "primary"}
                icon={btn.icon || ""}
                style={{ color: btn.color || "" }}
                disabled={!this.isActionButtonAllowed(page.action_buttons[btnType])}
                onClick={handleClick}>
                {btn.showProgress ? btn.progressLabel : btn.label || ""}
              </AntButton>
              </span>
            </AntTooltip>
          </div>
        );
      });
  }

  get breadCrumbs() {
    const { page_settings, location } = this.props;
    const page = page_settings[location.pathname];
    if (!page || !page.hasOwnProperty("bread_crumbs")) {
      return null;
    }

    const bread_crumbs = page?.bread_crumbs || [];

    return <BreadCrumbComponent breadcrumbs={bread_crumbs} />;
  }

  get defaultBreadCrumbs() {
    const { page, location } = this.props;

    let pathnames = location.pathname.split("/");
    const pathroot = pathnames[1]; //'admin'
    pathnames = pathnames.slice(2);
    const currentPage = this.currentPage;
    const labelMap = this.labelMap;

    if (!(pathnames.length > 1 && page.path !== "/dashboards" && currentPage.showBreadcrumb)) {
      return null;
    }
    return (
      <Breadcrumb aria-label="breadcrumb">
        {pathnames.map((value, index) => {
          const to = getHeaderBreadcrumbTo(page.id, pathroot, pathnames, index, location);
          if (pathnames.length - 1 === index) {
            return <Breadcrumb.Item key={index}>{labelMap[value]}</Breadcrumb.Item>;
          } else {
            if (page.path === "/dashboards" && this.props.enableDashboardRoutes === false) {
              return null;
            }
            return (
              <Breadcrumb.Item key={index}>
                <Crumb color="inherit" to={to} key={to} style={{ color: "var(--harness-blue)" }}>
                  {labelMap[value]}
                </Crumb>
              </Breadcrumb.Item>
            );
          }
        })}
      </Breadcrumb>
    );
  }

  get defaultBreadCrumbs() {
    const { page, location } = this.props;

    let pathnames = location.pathname.split("/");
    const pathroot = pathnames[1]; //'admin'
    pathnames = pathnames.slice(2);
    const currentPage = this.currentPage;
    const labelMap = this.labelMap;

    if (!(pathnames.length > 1 && page.path !== "/dashboards" && currentPage.showBreadcrumb)) {
      return null;
    }
    return (
      <Breadcrumb aria-label="breadcrumb">
        {pathnames.map((value, index) => {
          const to = getHeaderBreadcrumbTo(page.id, pathroot, pathnames, index, location);
          if (pathnames.length - 1 === index) {
            return <Breadcrumb.Item key={index}>{labelMap[value]}</Breadcrumb.Item>;
          } else {
            if (page.path === "/dashboards" && this.props.enableDashboardRoutes === false) {
              return null;
            }
            return (
              <Breadcrumb.Item key={index}>
                <RouterLink color="inherit" to={to} key={to} style={{ color: "var(--harness-blue)" }}>
                  {labelMap[value]}
                </RouterLink>
              </Breadcrumb.Item>
            );
          }
        })}
      </Breadcrumb>
    );
  }

  get divider() {
    const { page_settings, location } = this.props;
    const page = page_settings[location.pathname];
    const dividerStyle = { marginBottom: 0, marginTop: "18px" };
    if (!page || !page?.showDivider) {
      return null;
    }

    return <Divider style={dividerStyle} />;
  }

  get buttons() {
    const { page_settings, location, className } = this.props;
    const page = page_settings[location.pathname];
    if (!page || !Object.keys(page_settings).length) {
      return null;
    }
    return (
      <div className={`${className}__extra`} style={{ display: "flex" }}>
        {this.dropDownButtons}
        {this.actionButtons}
        {this.selectButtons}
        {window.isStandaloneApp && this.workspaceDropDownButtons}
      </div>
    );
  }

  get isNameWithAction() {
    const { page_settings, location } = this.props;
    const page = page_settings[location.pathname];
    return page && page.hasOwnProperty("nameWithMenuActions") && page.nameWithMenuActions?.length > 0;
  }

  get currentPageSettings() {
    const { page_settings, location } = this.props;
    return page_settings[location.pathname];
  }

  get currentPage() {
    const { page, location } = this.props;

    let currentPage = {};
    let pageItems = page.items;
    if (pageItems && pageItems.length) {
      currentPage =
        pageItems.find(item => {
          const _routePath = `${item.layout ?? page.layout}${page.path}` + item.path;
          const _pagePath = location.pathname;

          if (_routePath === _pagePath) {
            // Exact Match
            return true;
          }

          const _routePathParts = _routePath.split("/");
          const _pagePathParts = _pagePath.split("/");

          if (_routePathParts.length !== _pagePathParts.length) {
            // Cannot be a match due to different level of routes
            return false;
          }

          for (let _i = 0; _i < _routePathParts.length; _i++) {
            const _routePathPart = _routePathParts[_i];
            const _pagePathPart = _pagePathParts[_i];

            if ([":id", ":categoryId", ":accountId", ":projectIdentifier", ":orgIdentifier", ":dashboardId", ":widgetId", ":typeId"].includes(_routePathPart)) {
              // :id is special regex which matches with anything
              continue;
            }

            // TODO Add more regex here...

            if (_routePathPart !== _pagePathPart) {
              return false;
            }
          }

          // Control here means that in the above
          // foreach loop, all entries were a match
          return true;
        }) || {};
    }
    return currentPage;
  }

  get labelMap() {
    const { page, location } = this.props;
    const pageItems = page.items;

    //mapping url slugs to their labels; to be used while naming breadcrumbs
    let labelMap = {};
    labelMap[page.id] = getHeaderBreadcrumbLabel(page.id, location, page.label);
    let len = pageItems ? pageItems.length : 0;
    for (let i = 0; i < len; i++) {
      let item = pageItems[i];
      let path = item.path.substr(1);
      labelMap[path] = getHeaderBreadcrumbLabel(path, location, item.label);

      //TODO: fix here when routes from action buttons change
      let actionRoute = item.actionRoute;
      if (actionRoute) {
        labelMap[actionRoute] = item.actionLabel;
      }
    }
    return labelMap;
  }

  get backButton() {
    return (
      <Button
        className="mr-10 new-flow-back-btn"
        type="default"
        shape="circle"
        icon="arrow-left"
        onClick={() => this.props.history.goBack()}
      />
    );
  }

  get goBackLink() {
    const pageSettings = this.currentPageSettings;
    // support for go back even if bread_crumbs are present
    const hasGoBack = pageSettings?.hasOwnProperty("goBack") && pageSettings.goBack;
    if (pageSettings?.hasOwnProperty("bread_crumbs") && !hasGoBack) {
      const bread_crumbs = pageSettings?.bread_crumbs || [];
      if (bread_crumbs?.length > 1) {
        return bread_crumbs[bread_crumbs.length - 2]?.path || "generic";
      }
      return "generic";
    }
    return "generic";
  }

  get withBackButton() {
    const currentPage = this.currentPage;
    if (currentPage?.dynamicHeader) {
      return this.currentPageSettings?.withBackButton;
    }
    return currentPage?.withBackButton;
  }

  get dynamicNameAndDescription() {
    const { className } = this.props;
    const { header_title, header_description } = this.state;
    const currentPage = this.currentPage;

    const showDescription = currentPage.showDescription === true && currentPage.description?.length > 0;
    const nameWithAction = this.isNameWithAction && (
      <HeaderNameMenuAction
        handleClick={this.handleNameActionClick}
        menuItems={this.currentPageSettings?.nameWithMenuActions || []}
      />
    );
    const defaultTag = this.currentPageSettings?.defaultTag === true && (
      <AntTag className="mx-10" style={{ height: "fit-content" }} color="purple">
        Default
      </AntTag>
    );

    const title = (
      <div className="flex">
        {this.withBackButton && (
          <HeaderBackButton
            goBackCallback={this.currentPageSettings?.goBackCallback || undefined}
            link={this.goBackLink}
          />
        )}
        <AntTitle className={`${className}__title flex align-center`} level={4}>
          {header_title} {defaultTag} {nameWithAction}
        </AntTitle>
      </div>
    );

    return (
      <>
        {title}
        {showDescription && <AntText className={`${className}__description`}>{currentPage.description}</AntText>}
        {header_description && (
          <AntText className={`${className}__description  header_description`}>{header_description}</AntText>
        )}
      </>
    );
  }

  get nameAndDescription() {
    const { className, page } = this.props;
    const currentPage = this.currentPage;

    const showLabel = page.label && currentPage.showLabel !== false;
    const showDescription = currentPage.showDescription === true && currentPage.description?.length > 0;

    const title = showLabel && (
      <div className="flex align-center">
        {this.withBackButton && <HeaderBackButton link={this.goBackLink} />}
        <AntTitle className={`${className}__title`} level={4}>
          {currentPage.label ? currentPage.label : page.label ? page.label : ""}
        </AntTitle>
      </div>
    );

    return (
      <>
        {title}
        {showDescription && <AntText className={`${className}__description`}>{currentPage.description}</AntText>}
      </>
    );
  }

  get headerButtons() {
    const { className, selectedWorkspace } = this.props;
    const { currentPage } = this;
    if (currentPage.dynamicHeader) {
      return this.buttons;
    }
    if (currentPage.hasAction) {
      return (
        <div className={`${className}__extra`}>
          <AntButton
            type={currentPage.actionButtonType || "primary"}
            onClick={
              (!selectedWorkspace?.demo && currentPage.buttonHandler) ||
              (!selectedWorkspace?.demo && this.onActionClickHandler(currentPage.id))
            }>
            {currentPage.actionLabel}
          </AntButton>
        </div>
      );
    }
    if (currentPage.renderCustomHeader) {
      return <currentPage.renderCustomHeader />;
    }
  }

  handleNameActionClick = key => {
    const { location, setPageSettings } = this.props;
    const allItems = setMenuHeaderActions(this.currentPageSettings.nameWithMenuActions, key, true);
    setPageSettings(location.pathname, { ...(this.currentPageSettings || {}), nameWithMenuActions: allItems });
  };

  get isCustomDashboard() {
    return get(this.props.page_settings, [this.props.location.pathname, "custom_dashboard"], false);
  }

  get isWidgetExplorerFlow() {
    return get(this.props.page_settings, [this.props.location.pathname, "newWidgetExplorerHeader"], false);
  }

  get showBottomSeparator() {
    return get(this.props.page_settings, [this.props.location.pathname, "showBottomSeparator"], false);
  }

  get showFullScreenBottomSeparator() {
    return get(this.props.page_settings, [this.props.location.pathname, "showFullScreenBottomSeparator"], false);
  }

  get content() {
    if (this.isWidgetExplorerFlow) {
      return this.newWidgetFlowContent;
    }
    return this.oldContent;
  }

  get newWidgetFlowContent() {
    return (
      <>
        {this.breadCrumbs}
        {this.defaultBreadCrumbs}
        <div className="new-content">
          <div className="name-container">
            {this.backButton}
            {this.dynamicNameAndDescription}
          </div>
          {this.headerButtons}
        </div>
      </>
    );
  }

  get bottomSeparator() {
    if (!this.showBottomSeparator) {
      return null;
    }
    return <hr className="bottom-separator" />;
  }

  get oldContent() {
    const currentPage = this.currentPage;
    let nameAndDescription = currentPage.dynamicHeader ? this.dynamicNameAndDescription : this.nameAndDescription;

    const breadCrumbPosition = this.currentPageSettings?.bread_crumbs_position || "after";

    // bread_crumbs_position = "after" => breadcrumbs after the name and description
    // bread_crumbs_position = "before" => breadcrumbs before the name and description

    const header =
      breadCrumbPosition === "after" ? (
        <>
          {nameAndDescription}
          {this.breadCrumbs}
          {this.bottomSeparator}
        </>
      ) : (
        <>
          {this.breadCrumbs}
          {nameAndDescription}
          {this.bottomSeparator}
        </>
      );

    return (
      <>
        {header}
        {this.defaultBreadCrumbs}
      </>
    );
  }

  render() {
    const { className } = this.props;
    const headerClassName = this.currentPageSettings?.headerClassName;
    if (isDashboardViewPage() || isScoreDashboard() || isHarnessHomePage() || isIntegrationMapping()) return null;
    if (this.props.page.newHeader) {
      return React.createElement(this?.props?.page?.newHeader);
    }
    return (
      <Header
        className={cx(`flex direction-column ${className} ${headerClassName}`, {
          "pb-3": this.isCustomDashboard,
          "pb-30": !this.showBottomSeparator,
          "pb-15": this.showBottomSeparator,
          "full-with-bottom-border": this.showFullScreenBottomSeparator
        })}>
        <div className={`flex direction-row justify-space-between`}>
          <div
            className={cx(`flex direction-column dashboard-search-container`, {
              [`${className}__info`]: !this.isWidgetExplorerFlow,
              "new-content-container": this.isWidgetExplorerFlow
            })}>
            {this.content}
          </div>
          {!this.isWidgetExplorerFlow && this.headerButtons}
        </div>
        {this.divider}
      </Header>
    );
  }
}

HeaderContainer.propTypes = {
  page: PropTypes.shape({
    label: PropTypes.string,
    description: PropTypes.string,
    location: PropTypes.any,
    actions: PropTypes.any,
    actionLabel: PropTypes.string,
    hasAction: PropTypes.bool,
    onClickEvent: PropTypes.func
  }),
  enableDashboardRoutes: PropTypes.bool.isRequired,
  toggleFullscreenModal: PropTypes.func.isRequired,
  location: PropTypes.shape({
    pathname: PropTypes.string.isRequired
  }).isRequired,
  page_settings: PropTypes.any,
  selectedWorkspace: PropTypes.any,
  history: PropTypes.any
};

HeaderContainer.defaultProps = {
  className: "new-page-header",
  page: {
    label: "",
    description: "",
    actionLabel: "",
    hasAction: false,
    onClickEvent: () => {}
  },
  enableDashboardRoutes: true
};

const mapStateToProps = state => ({
  page_settings: getPageSettingsSelector(state),
  selectedWorkspace: getSelectedWorkspace(state)
});

export const mapDispatchToProps = dispatch => ({
  toggleFullscreenModal: (page, isOpen) => dispatch(toggleFullscreenModalAction(page, isOpen)),
  ...mapPageSettingsDispatchToProps(dispatch)
});

export default connect(mapStateToProps, mapDispatchToProps)(HeaderContainer);
