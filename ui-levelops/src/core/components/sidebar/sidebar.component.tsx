import { Layout, Tooltip } from "antd";
import { get } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { NavLink, RouteComponentProps, useHistory } from "react-router-dom";
import { USERROLES } from "routes/helper/constants";
import LocalStoreService from "services/localStoreService";
import { AntText, Avatar, IconButton, NameAvatar, SvgIcon } from "shared-resources/components";
import { DropdownWrapperHelper } from "shared-resources/helpers";
import { DEFAULT_DASHBOARD_KEY, NO_DEFAULT_DASH_ID, TENANT_STATE } from "../../../dashboard/constants/constants";
import { dashboardDefault, tenantStateGet } from "reduxConfigs/actions/restapi";
import { defaultDashboardSelector } from "reduxConfigs/selectors/dashboardSelector";
import { useHasEntitlements } from "./../../../custom-hooks/useHasEntitlements";
import routes from "../../../routes/routes";
import { Entitlement } from "./../../../custom-hooks/constants";
import { isSelfOnboardingUser, sessionUserLastLoginURL } from "reduxConfigs/selectors/session_current_user.selector";
import "./sidebar.style.scss";
import { WORKITEMS, REPORTS, TRIAGE, TABLE_CONFIGS, PRODUCTS, TEMPLATES, PROPELS } from "./constant";
import { DOCS_ROOT } from "constants/docsPath";
import { removeAdminLayoutForLinks } from "utils/dashboardUtils";
import { getBaseUrl, ORGANIZATION_USERS_ROUTES, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";

const { Sider } = Layout;
interface SidebarComponentProps extends RouteComponentProps {
  className?: string;
  enableDashboardRoutes: boolean;
  avatarUrl?: string;
  rbac: string;
  onLogoutEvent: any;
  onNavigateToProfileEvent: any;
}

const SidebarComponent: React.FC<SidebarComponentProps> = (props: SidebarComponentProps) => {
  let triggerElement: any = null;
  const className = props.className || "new-sidebar";
  const avatarUrl = props.avatarUrl || "";
  const enableDashboardRoutes = props.enableDashboardRoutes || true;
  const isTrialUser = useSelector(isSelfOnboardingUser);
  const [isMenuOpened, setMenuOpened] = useState(false);
  const [defaultDashboardId, setDefaultDashboardId] = useState<null | string>(null);
  const dispatch = useDispatch();
  const defaultDashboardState = useSelector(defaultDashboardSelector);
  const entDashboard = useHasEntitlements(Entitlement.DASHBOARDS);
  const entDashboardRead = useHasEntitlements(Entitlement.DASHBOARDS_READ);
  const entPropels = useHasEntitlements(Entitlement.PROPELS);
  const entPropelsRead = useHasEntitlements(Entitlement.PROPELS_READ);
  const sessionLastLoginURL = useSelector(sessionUserLastLoginURL);
  const localStorage = new LocalStoreService();

  const history = useHistory();
  const userName = localStorage.getUserName();
  const userEmail = localStorage.getUserEmail();

  const changesSelector = useSelector(unSavedChangesSelector);

  useEffect(() => {
    dispatch(dashboardDefault(DEFAULT_DASHBOARD_KEY));
    dispatch(tenantStateGet(TENANT_STATE));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const { error, loading } = defaultDashboardState;
    if (loading !== undefined && error !== undefined && loading !== true && error !== true) {
      const exists = get(defaultDashboardState, ["data", "exists"], false);
      const id = get(defaultDashboardState, ["data", "id"], false);
      if (exists) {
        setDefaultDashboardId(id);
      } else {
        setDefaultDashboardId(NO_DEFAULT_DASH_ID);
      }
    }
  }, [defaultDashboardState]); // eslint-disable-line react-hooks/exhaustive-deps

  const onLogoClickHandler = useCallback(() => {
    props.history.push(getBaseUrl());
  }, [defaultDashboardId]); // eslint-disable-line react-hooks/exhaustive-deps

  const openDocs = useCallback(() => {
    window.open(DOCS_ROOT, "_blank");
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const setTriggerElement = useCallback((ref: any) => {
    triggerElement = ref; // eslint-disable-line react-hooks/exhaustive-deps
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const getClasses = useCallback(
    (path: string) => {
      const classes = [`${className}__item`];
      if (props.location.pathname.includes(path)) {
        classes.push(`${className}__item--selected`);
      }
      return classes.join(" ");
    },
    [className, props.location.pathname]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const getSidebarItemContainerClasses = useCallback(
    (path: string) => {
      let classes: string[] = [];
      if (props.location.pathname.includes(path)) {
        classes.push("sidebar-item-wrapper--selected");
      }
      return classes.join(" ");
    },
    [className, props.location.pathname]
  );

  const toggleUserMenuHandler = useCallback(() => {
    setMenuOpened(!isMenuOpened);
  }, [isMenuOpened]); // eslint-disable-line react-hooks/exhaustive-deps

  const sidebarItems = useMemo(() => {
    const rbac = props.rbac;
    switch (rbac) {
      case USERROLES.SUPER_ADMIN:
      case "admin":
        return [
          "home",
          ...(entDashboard || entDashboardRead ? ["dashboard"] : []),
          "workitems",
          "reports",
          "triage",
          "table-configs",
          ...(entPropels || entPropelsRead ? ["propels"] : []),
          "products",
          "templates",
          "configuration"
        ];
      case "limited_user":
        return ["reports", "workitems"];
      case USERROLES.ASSIGNED_ISSUES_USER:
        return ["workitems"];
      case "auditor":
        return ["reports"];
      case "restricted_user":
        return [];
      case "public_dashboard":
        return ["home", "dashboard"];
      case USERROLES.ORG_UNIT_ADMIN:
        return ["home", "dashboard", "configuration"];
      default:
        return ["reports"];
    }
  }, [props.rbac, entPropels, entDashboard, entDashboardRead, entPropelsRead]); // eslint-disable-line react-hooks/exhaustive-deps

  const sidebarRoutes = useMemo(() => {
    const items: any[] = [];
    sidebarItems.map((i: any) => (items[i] = {}));
    const filteredRoutes = routes().reduce((carry, route: any) => {
      if (route.hide !== true && route.rbac && route.rbac.includes(props.rbac) && sidebarItems.includes(route.id)) {
        carry[route.id] = route;
      }
      return carry;
    }, items);
    return Object.values(filteredRoutes);
  }, [sidebarItems]); // eslint-disable-line react-hooks/exhaustive-deps

  const renderAvatar = useMemo(
    () => (
      <div className={`centered ${className}__avatar mb-20`} ref={setTriggerElement} role="presentation">
        {userEmail ? (
          <div className="avatar-wrap" onClick={toggleUserMenuHandler}>
            <NameAvatar name={userEmail} showTooltip={false} />
          </div>
        ) : (
          <Avatar avatar={avatarUrl} onClickEvent={toggleUserMenuHandler} size="large" />
        )}
      </div>
    ),
    [avatarUrl, className] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const renderDocumentation = useMemo(
    () => (
      <div className={`centered ${className}__help`}>
        <IconButton
          className="flex icon-button"
          icon="read"
          onClickEvent={openDocs}
          style={{ height: "1.75rem", width: "1.75rem" }}
        />
        <AntText className="route-label">Docs</AntText>
      </div>
    ),
    [className] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const style = useMemo(() => ({ height: "1.5rem", width: "1.5rem" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const validateAndRedirect = (link: string) => {
    if (
      changesSelector?.dirty &&
      [`${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`, `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}`].includes(
        props.location.pathname
      )
    ) {
      dispatch(
        setUnSavedChanges({
          show_modal: true,
          dirty: true,
          onCancel: () => {
            dispatch(setUnSavedChanges({ show_modal: false, dirty: false, onCancel: "" }));
            history.push(link);
          }
        })
      );
    } else {
      history.push(link);
    }
  };

  const renderLogo = useMemo(
    () => (
      <div className={`centered ${className}__logo`}>
        <IconButton
          className="flex icon-button"
          icon="sdiIcon"
          onClickEvent={() => validateAndRedirect(getBaseUrl())}
        />
      </div>
    ),
    [onLogoClickHandler, className, changesSelector] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const renderNavItems = useMemo(
    () => (
      <div className={`${className}__items`}>
        {sidebarRoutes.map((route: any, index: number) => {
          let path = route.path;
          let labelPath = route.labelPath;
          let currentLabel: string =
            props.location.pathname.includes(path) || props.location.pathname.includes(labelPath) ? route.label : "";
          const routeNames = [WORKITEMS, REPORTS, TRIAGE, TABLE_CONFIGS, PRODUCTS, PROPELS, TEMPLATES];
          if (isTrialUser && routeNames.includes(route.id)) {
            return <></>;
          }
          if (route.id === "dashboard" && enableDashboardRoutes) {
            path = removeAdminLayoutForLinks(sessionLastLoginURL);
          }
          return (
            <div className={`sidebar-nav ${getSidebarItemContainerClasses(route.layout + route.path)}`}>
              <Tooltip key={`sidebarRoutes-${index}`} title="" placement="right" trigger="hover">
                <NavLink
                  to={`${route.layout}${path}`}
                  className={`direction-column centered ${getClasses(route.layout + route.path)}`}
                  key={route.path}
                  onClick={e => {
                    e.preventDefault();
                    validateAndRedirect(`${route.layout}${path}`);
                  }}>
                  <div className={`sidebar-item-wrapper`}>
                    <SvgIcon icon={route.icon} style={style} />
                    <Tooltip key={`sidebarRoutes-${index}-${route.icon}`} title="" placement="right" trigger="hover">
                      <AntText className="route-label">{route.label}</AntText>
                    </Tooltip>
                  </div>
                </NavLink>
              </Tooltip>
            </div>
          );
        })}
      </div>
    ),
    [
      className,
      defaultDashboardId,
      enableDashboardRoutes,
      props.location.pathname,
      isTrialUser,
      sidebarRoutes,
      changesSelector
    ] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const renderUserDropdown = useMemo(() => {
    if (!isMenuOpened) {
      return null;
    }
    return (
      <DropdownWrapperHelper
        triggerElement={triggerElement}
        onClose={toggleUserMenuHandler}
        style={{ bottom: 50, left: 40 }}>
        <div className="user-menu">
          <p className="user-name">{userName}</p>
          <div className="divider" />
          <div className="user-menu__option" role="presentation" onClick={props.onNavigateToProfileEvent}>
            Profile
          </div>
          <div className="user-menu__option" role="presentation" onClick={props.onLogoutEvent}>
            Log out
          </div>
        </div>
      </DropdownWrapperHelper>
    );
  }, [isMenuOpened, triggerElement, props.onLogoutEvent, props.onNavigateToProfileEvent]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Sider width={88} className={`${className}`}>
      {renderLogo}
      {renderNavItems}
      {renderDocumentation}
      {renderAvatar}
      {renderUserDropdown}
    </Sider>
  );
};

export default React.memo(SidebarComponent);
