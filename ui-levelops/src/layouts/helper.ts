import { getBaseUrl } from "constants/routePaths";
import { get } from "lodash";
import queryString from "query-string";

export const validateUrl = (location: any) => {
  const pathName = get(location, "pathname", "");
  const isDashboardListPage = pathName.endsWith('/dashboards/list');
  const isCreateDashboardPage = pathName.endsWith('/dashboards/create');
  const isDemoDashboardPage = pathName.endsWith('/dashboards/demo-dashboards');
  const isDashboardPage = pathName.endsWith('dashboards');
  const isIntegrationMapping = pathName.endsWith('sei-integration-mapping');
  const isHomePage = pathName.endsWith('home');
  const isTrialPage = pathName.endsWith('trail');
  const validUrl = isCreateDashboardPage || isDemoDashboardPage || isDashboardPage || isHomePage || isTrialPage || isIntegrationMapping || isDashboardListPage;
  const isdashboardUrl = pathName.includes("dashboards");
  const drillDownURL = pathName.includes("drill-down");
  const ticketDetailsURL = pathName.includes("ticket_details");
  const scorecardURL = pathName.includes("scorecard");
  const devProductivityURL = pathName.includes("dev_productivity");
  const dashboardURLNotChanged = drillDownURL || ticketDetailsURL || scorecardURL || devProductivityURL;
  let invalidURL = false;
  const localUrl = location.pathname.concat(location.search);
  if (localUrl === getBaseUrl()) {
    invalidURL = true;
  }
  if (isdashboardUrl && !validUrl && !dashboardURLNotChanged) {
    const search = location.search;
    const OU = queryString.parse(search)?.OU as string;
    if (!OU) {
      invalidURL = true;
    }
  }
  return invalidURL;
};

export const isWorkspaceNeeded = (location: any) => {
  const pathName = get(location, "pathname", "");
  const isdashboardUrl = pathName.includes("dashboards");
  const drillDownURL = pathName.includes("drill-down");
  const ticketDetailsURL = pathName.includes("ticket_details");
  const scorecardURL = pathName.includes("scorecard");
  const devProductivityURL = pathName.includes("dev_productivity");
  const dashboardURLNotChanged = drillDownURL || ticketDetailsURL || scorecardURL || devProductivityURL;
  let needed = false;

  if (isdashboardUrl && !dashboardURLNotChanged) {
    needed = true;
  }
  return needed;
};
