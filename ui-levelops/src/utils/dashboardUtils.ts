import { NO_DEFAULT_DASH_ID } from "../dashboard/constants/constants";
import { DASHBOARD_ROUTES, getBaseUrl } from "../constants/routePaths";
import { RestDashboard, RestWidget } from "../classes/RestDashboards";
import { WebRoutes } from "../routes/WebRoutes";
import { capitalize } from "lodash";
import { toTitleCase } from "./stringUtils";
import { ProjectPathProps } from "classes/routeInterface";

export function getDefaultDashboardPath(projectPathParams: ProjectPathProps, defaultDashboardId: string | null, urlPrefix: string | null = null) {
  if (!defaultDashboardId) {
    defaultDashboardId = NO_DEFAULT_DASH_ID;
  }
  let path = "";
  if (!urlPrefix) {
    urlPrefix = `${getBaseUrl(projectPathParams)}${DASHBOARD_ROUTES._ROOT}`;
  }
  if (defaultDashboardId === NO_DEFAULT_DASH_ID) {
    path = `${urlPrefix}${defaultDashboardId}`;
  } else {
    path = `${urlPrefix}/${defaultDashboardId}`;
  }
  return path;
}

export function generateDashboardLayoutPageBreadcrumbs(
  projectPathParams: ProjectPathProps,
  dashboard: RestDashboard,
  widget: RestWidget | null,
  search?: string
) {
  const { id: dashboardId, name } = dashboard;
  let modifyLayoutUrl = WebRoutes.dashboard.widgets.widgetsRearrange(projectPathParams, dashboardId, search);
  let breadcrumbs = [
    {
      label: name,
      path: WebRoutes.dashboard.details(projectPathParams, dashboardId, search)
    }
  ];
  breadcrumbs.push({
    label: "Modify Layout",
    path: modifyLayoutUrl
  });
  return breadcrumbs;
}

export function generateConfigureWidgetPageBreadcrumbs(
  projectPathParams: ProjectPathProps,
  dashboardId: string,
  dashboardName: string,
  widgetId: string,
  widgetName: string,
  search?: string
) {
  return [
    {
      label: dashboardName,
      path: WebRoutes.dashboard.details(projectPathParams, dashboardId, search)
    },
    {
      label: widgetName,
      path: WebRoutes.dashboard.widgets.details(projectPathParams, dashboardId, widgetId)
    }
  ];
}

export function isDashboardViewPage() {
  const path = window.location.pathname;
  const dashboardPath = '/dashboards';
  const dashboardPathIndex = path.indexOf(dashboardPath)
  if (dashboardPathIndex != -1) {
    const dashboardNo = path.substring(dashboardPathIndex + dashboardPath.length)
    const dashboardViewRegex =/^\/\d+$/;
    return dashboardViewRegex.test(dashboardNo);
  }
  return false;
}

export function isScoreDashboard() {
  const pathname = window.location.pathname;
  return pathname.includes("scorecard") || pathname.includes("dev_productivity");
}

export function isHarnessHomePage() {
  const pathName = window.location.pathname;
  return pathName.endsWith('/home') || pathName.endsWith('/trial')
}

export function generateExploreWidgetPageBreadcrumbs(
  projectPathParams: ProjectPathProps,
  dashboardId: string,
  dashboardName: string,
  pageName: string,
  search?: string
) {
  return [
    {
      label: dashboardName,
      path: WebRoutes.dashboard.details(projectPathParams, dashboardId, search)
    },
    {
      label: pageName,
      path: WebRoutes.dashboard.widgets.widgetsExplorer(projectPathParams, dashboardId, search)
    }
  ];
}

export function generateExploreWidgetByThemePageBreadcrumbs(
  projectPathParams: ProjectPathProps,
  dashboardId: string,
  dashboardName: string,
  pageName: string,
  theme: string,
  search?: string
) {
  const title = theme
    .split("_")
    .map((str: string) => capitalize(str))
    .join(" ");
  return [
    {
      label: dashboardName,
      path: WebRoutes.dashboard.details(projectPathParams, dashboardId, search)
    },
    {
      label: pageName,
      path: WebRoutes.dashboard.widgets.widgetsExplorer(projectPathParams, dashboardId, search)
    },
    {
      label: title,
      path: WebRoutes.dashboard.widgets.widgetsExploreByCategory(projectPathParams, dashboardId, theme, search)
    }
  ];
}

export function generateExploreWidgetByThemeCustomCategoryPageBreadcrumbs(
  projectPathParams: ProjectPathProps,
  dashboardId: string,
  dashboardName: string,
  pageName: string,
  theme: string,
  search?: string
) {
  const title = toTitleCase(theme);
  return [
    {
      label: dashboardName,
      path: WebRoutes.dashboard.details(projectPathParams, dashboardId, search)
    },
    {
      label: pageName,
      path: WebRoutes.dashboard.widgets.widgetsExplorer(projectPathParams, dashboardId, search)
    },
    {
      label: title,
      path: WebRoutes.dashboard.widgets.widgetsExploreByCustomCategory(projectPathParams, dashboardId, theme, search)
    }
  ];
}
/** this function converts /admin/home to /home */
export const removeAdminLayoutForLinks = (url: string) => {
  if (!url.includes(getBaseUrl())) return url;
  const splittedURL = url.split(getBaseUrl());
  if (splittedURL.length > 1) {
    return splittedURL[1];
  }
  return url;
};
