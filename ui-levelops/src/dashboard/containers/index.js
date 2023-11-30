import lazyWithRetry from "./lazyWithRetry";

export { default as DashboardOverview } from "./dashboard-overview.container";
export { default as DashboardIntegrations } from "./dashboard-integrations.container";
export { default as DashboardGraphsContainer } from "./dashboard-graphs.container";
export { default as DashboardTicketsContainer } from "./dashboard-tickets.container";
export { default as DashboardRearrange } from "./DashboardRearrangeContainer";
export { default as DashboardApplicationFiltersModal } from "./dashboard-application-filters-container/dashboard-application-filters.container";

export const LazyLoadedDashboardRearrange = lazyWithRetry(
  () => import("./DashboardRearrangeContainer"),
  "DashboardRearrangeContainer"
);
export const LazyLoadedWidgetCreatePage = lazyWithRetry(
  () => import("./../../dashboard/pages/configure-widget/CreateWidgetPage"),
  "CreateWidgetPage"
);
export const LazyLoadedWidgetExplorerPage = lazyWithRetry(
  () => import("../pages/explore-widget/widget-themes-list/WidgetExplorerPage"),
  "WidgetExplorerPage"
);
export const LazyLoadedWidgetByThemePage = lazyWithRetry(
  () => import("../pages/explore-widget/widget-by-theme/WidgetsByTheme"),
  "WidgetsByTheme"
);
export const LazyLoadedWidgetByCustomCategoryPage = lazyWithRetry(
  () => import("../pages/explore-widget/widget-by-theme/WidgetsByThemeCustomCategory"),
  "WidgetsByThemeCategory"
);
export const LazyLoadedWidgetEditPage = lazyWithRetry(
  () => import("./../../dashboard/pages/configure-widget/EditWidgetPage"),
  "EditWidgetPage"
);
