import { DashboardList } from "dashboard";
import { DashboardEditCreate, DashboardView, TicketBounce } from "dashboard/pages";
import { DashboardTicketsContainer } from "dashboard/containers";
import DashboardWidgetsContainer from "../dashboard/containers/DashboardWidgetsContainer";
import ScoreCardDashboardViewPage from "dashboard/pages/scorecard/containers/ScorecardDashbaordView.container";
import ScoreCardDashboardDrilldown from "../dashboard/pages/scorecard/containers/ScorecardDashboardDrillDown.container";
import DevProductivityDashboard from "dashboard/pages/dev-productivity-report/container/DevProductivityDashboard";
import DemoDevProductivityDashboard from "dashboard/pages/demo-dev-productivity-report/container/DemoDevProductivityDashboard";
import DemoScoreCardDashboardViewPage from "dashboard/pages/demo-dev-productivity-score-report/containers/DemoScoreCardDashboardViewPage";
import { USERROLES, USER_ADMIN_ROLES } from "./helper/constants";
import { getBaseUrl } from 'constants/routePaths';
import { projectPathPropsDef } from 'utils/routeUtils'
import LandingPageContainer from "views/Pages/LandingPageContainer";

const RBAC_FULL_ACCESS = [...USER_ADMIN_ROLES, "limited_user", "restricted_user", "auditor", "assigned_issues_user"];
export function dashboardRoutes(hideDashboards) {
  return [
    {
      name: "home",
      path: "/dashboards",
      layout: getBaseUrl(projectPathPropsDef),
      rbac: [...USER_ADMIN_ROLES, USERROLES.LIMITED_USER, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      mini: "home",
      component: LandingPageContainer,
      collapse: false,
      icon: "home",
      id: "home",
      label: "Select a Category to continue",
      hide: hideDashboards,
      rbac: [...USER_ADMIN_ROLES, "limited_user", "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
      actions: {},
      items: [
        {
          // Dashboards List
          path: "/list",
          rbac: [...USER_ADMIN_ROLES, "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
          description: "View and manage Insights",
          label: "Dashboards",
          actionId: "dashboards-add",
          actionRoute: "dashboards/create",
          actionLabel: "New Dashboard",
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true,
          showLabel: true,
          hasAction: true,
        },
        {
          // Dashboards Create
          path: "/create",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage Insights",
          label: "New",
          showLabel: true,
          hasAction: false,
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        },
        {
          // Dashboards View
          path: "/:dashboardId",
          rbac: [...USER_ADMIN_ROLES, "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
          description: "View and manage Insights",
          label: "New",
          showLabel: true,
          hasAction: true,
          actionId: "dashboards-view",
          actionLabel: "Edit",
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        },
        {
          // Dashboards View
          path: "/:dashboardId/modify",
          rbac: USER_ADMIN_ROLES,
          description: "Modify Dashboards",
          label: "Modify",
          hasAction: true,
          actionId: "dashboards-modify",
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        },
        {
          // Dashboards Jira Tickets
          path: "/drill-down",
          rbac: USER_ADMIN_ROLES,
          description: "Drill Down",
          label: "Drill Down",
          showLabel: true,
          hasAction: false,
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        },
        {
          path: "/:dashboardId/widgets/:widgetId",
          rbac: RBAC_FULL_ACCESS,
          description: "Configure Widgets",
          label: "Configure Widgets",
          showLabel: true,
          hasAction: true,
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        },
        {
          path: "/:dashboardId/widgets/:widgetId/new",
          rbac: RBAC_FULL_ACCESS,
          description: "Configure Widgets",
          label: "Edit Widgets",
          showLabel: true,
          hasAction: true,
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        },
        {
          path: "/:dashboardId/widgets/explorer/custom/dora",
          rbac: RBAC_FULL_ACCESS,
          description: "Configure Widgets",
          label: "Edit Widgets",
          showLabel: true,
          hasAction: true,
          layout: getBaseUrl(projectPathPropsDef),
          dynamicHeader: true
        }
      ]
    },
    {
      name: "Dashboards",
      path: "/dashboards/list",
      layout: getBaseUrl(projectPathPropsDef),
      component: DashboardList,
      hide: hideDashboards,
      rbac: [...USER_ADMIN_ROLES, "limited_user", "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
      collapse: false,
      icon: "overview",
      id: "dashboards",
      label: "All Insights",
      dynamicHeader: true,
    },
    {
      name: "Create dashboard",
      path: "/dashboards/create",
      layout: getBaseUrl(projectPathPropsDef),
      component: DashboardEditCreate,
      hide: hideDashboards,
      rbac: USER_ADMIN_ROLES,
      collapse: false
    },
    {
      name: "Dashboard_Tickets_List",
      path: "/dashboards/drill-down",
      layout: getBaseUrl(projectPathPropsDef),
      component: DashboardTicketsContainer,
      rbac: [...RBAC_FULL_ACCESS, "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
      collapse: false,
      showLabel: false
    },
    {
      name: "Jira Ticket Details",
      path: "/dashboards/ticket_details",
      layout: getBaseUrl(projectPathPropsDef),
      component: TicketBounce,
      rbac: [...RBAC_FULL_ACCESS, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      collapse: false,
      showLabel: false
    },
    {
      name: "Engineer Snapshot",
      path: "/dashboards/scorecard",
      layout: getBaseUrl(projectPathPropsDef),
      component: ScoreCardDashboardViewPage,
      rbac: [...RBAC_FULL_ACCESS, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      label: "Engineer Snapshot",
      collapse: false,
      showLabel: false
    },
    {
      name: "Engineer Snapshot",
      path: "/dashboards/demo_scorecard",
      layout: getBaseUrl(projectPathPropsDef),
      component: DemoScoreCardDashboardViewPage,
      rbac: [...RBAC_FULL_ACCESS, USERROLES.PUBLIC_DASHBOARD, , USERROLES.ORG_UNIT_ADMIN],
      label: "Engineer Snapshot",
      collapse: false,
      showLabel: false
    },
    {
      name: "Feature Drilldown",
      path: "/dashboards/scorecard/drilldown",
      layout: getBaseUrl(projectPathPropsDef),
      component: ScoreCardDashboardDrilldown,
      rbac: [...RBAC_FULL_ACCESS, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      label: "Engineer Snapshot",
      collapse: false,
      showLabel: false
    },
    {
      name: "Dev Productivity Dashboard",
      path: "/dashboards/dev_productivity",
      layout: getBaseUrl(projectPathPropsDef),
      component: DevProductivityDashboard,
      rbac: [...RBAC_FULL_ACCESS, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      label: "Dev Productivity Dashboard",
      collapse: false,
      showLabel: false
    },
    {
      name: "Dev Productivity Dashboard",
      path: "/dashboards/demo_dev_productivity",
      layout: getBaseUrl(projectPathPropsDef),
      component: DemoDevProductivityDashboard,
      rbac: [...RBAC_FULL_ACCESS, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      label: "Dev Productivity Dashboard",
      collapse: false,
      showLabel: false
    },
    {
      name: "dashboard",
      path: "/dashboards/:dashboardId",
      layout: getBaseUrl(projectPathPropsDef),
      component: DashboardView,
      hide: hideDashboards,
      rbac: [...RBAC_FULL_ACCESS, "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
      collapse: false,
      showLabel: false
    },
    {
      name: "dashboard",
      path: "/dashboards/:dashboardId/widgets",
      layout: getBaseUrl(projectPathPropsDef),
      component: DashboardWidgetsContainer,
      hide: hideDashboards,
      rbac: RBAC_FULL_ACCESS,
      exact: false,
      dynamicHeader: true
    }
  ];
}
