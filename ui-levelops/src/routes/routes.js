import AnswerQuestionnaireContainer from "assessments/containers/answer-questionnaire.container";
import { IntegrationAdd } from "configurations/pages";
import ConnectTools from "configurations/pages/onboarding/ConnectTools/ConnectTools";
import { PluginResultContainer } from "configurations/pages";
import { DashboardContainer } from "dashboard/pages";
import { ProductDetail, ProductsList, WorkspaceList } from "products";
import { ProfileEdit } from "profile";
import { IntegrationReportDetails, PropelReportDetail, Reports } from "reports/pages";
import { SignatureLogsList } from "results";
import { SDLCFlows } from "sdlc-flow/pages";
import { WorkflowEditor, WorkflowList } from "workflow";
import ErrorNotFound from "../components/Error/ErrorNotFound";
import { EmptyPage, ResetPasswordPage, LoginPage } from "views/Pages";
import { configTableRoutes } from "./config-tables.routes";
import { configurationRoutes } from "./configuration.routes";
import { dashboardRoutes } from "./dashboard.routes";
import { FailureAnalysisRoutes } from "./failureanalysis.routes";
import { issuesRoutes } from "./issues.routes";
import { templateRoutes } from "./templates.routes";
import { triageRoutes } from "./triage.routes";
import { propelRoutes } from "./propels.routes";
import { AddJenkinsIntegrationNodes } from "../configurations/pages/integrations";
import { BASE_UI_URL } from "helper/envPath.helper";
import LandingPageHeader from "views/Pages/landing-page/LandingPageHeader";
import { WORKSPACES } from "dashboard/constants/applications/names";
import { WORKSPACE_ADD_PATH, WORKSPACE_EDIT_PATH, WORKSPACE_PATH } from "constants/routePaths";
import NoOUDefaultDashboard from "dashboard/pages/dashboard-view/NoOUDefaultDashboard";
import { USERROLES, USER_ADMIN_ROLES } from "./helper/constants";
import LandingPageContainer from "views/Pages/LandingPageContainer";
import DashboardCustomOU from "dashboard/components/dashboard-header/DashboardCustomOU/dashboard-custom-ou-wrapper";
import DemoDashboardRedirectComponent from "dashboard/pages/demo-dashboards/DemoDashboardRedirectComponent";
import envConfig from "env-config";
import { getDashboardsPage, getBaseUrl, getReportsPage } from "constants/routePaths";
import { projectPathPropsDef } from "utils/routeUtils";
import { HarnessRoutes } from "./harness.routes";

function routes({
  hideAppDashboard = envConfig.get("DASHBOARD") === "hide",
  hideAppSDLCFlows = envConfig.get("SDLC_FLOWS") === "hide",
  hideAppSTT = envConfig.get("STT") === "hide",
  hideAppWorkflows = envConfig.get("WORKFLOWS") === "hide",
  hideAppSignatures = envConfig.get("SIGNATURES") === "hide",
  hideAppViolation = envConfig.get("VIOLATION_LOGS") === "hide",
  hideAppTools = envConfig.get("TOOLS") === "hide",
  hideDashboards = false
} = {}) {
  return [
    ...HarnessRoutes(),
    ...dashboardRoutes(hideDashboards),
    ...FailureAnalysisRoutes(),
    {
      name: "Dashboard",
      path: "/dashboard",
      layout: getBaseUrl(projectPathPropsDef),
      rbac: [...USER_ADMIN_ROLES, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      component: DashboardContainer,
      hide: hideAppDashboard,
      collapse: false,
      id: "dashboard",
      label: "Insights",
      fullPath: getDashboardsPage(projectPathPropsDef),
      icon: "overview",
      actions: {},
      labelPath: "/dashboards",
      items: [
        {
          id: "dashboard",
          path: "",
          label: "Insights",
          description: "",
          //showLabel: false,
          hasAction: !hideDashboards,
          actionRoute: "",
          actionLabel: "All Insights",
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(getDashboardsPage(projectPathPropsDef)));
          }
        }
      ]
    },
    {
      path: "/demo-dashboard",
      layout: getBaseUrl(projectPathPropsDef),
      component: DemoDashboardRedirectComponent,
      hide: hideDashboards,
      rbac: [...USER_ADMIN_ROLES, "limited_user", "public_dashboard", USERROLES.ORG_UNIT_ADMIN],
      collapse: false,
      id: "demo-dashboard",
      fullPath: `${getBaseUrl(projectPathPropsDef)}/demo-dashboard`
    },
    {
      name: "Dashboard",
      path: "",
      layout: getBaseUrl(projectPathPropsDef),
      rbac: USER_ADMIN_ROLES,
      component: DashboardContainer,
      hide: hideAppDashboard,
      collapse: false
    },
    {
      name: "Not Authorized",
      path: "/not-authorized",
      layout: getBaseUrl(),
      rbac: ["admin", "limited_user", "auditor", "restricted_user", USERROLES.SUPER_ADMIN],
      component: EmptyPage,
      collapse: false
    },
    {
      path: "/answer-questionnaire-page",
      layout: getBaseUrl(),
      name: "Complete Assessment",
      mini: "AQ",
      component: AnswerQuestionnaireContainer,
      invisible: true,
      rbac: [
        ...USER_ADMIN_ROLES,
        USERROLES.AUDITOR,
        USERROLES.LIMITED_USER,
        USERROLES.RESTRICTED_USER,
        USERROLES.ASSIGNED_ISSUES_USER
      ]
    },
    {
      name: "Reports",
      path: "/reports",
      layout: getBaseUrl(),
      component: Reports,
      //hide: options.reactAppDashboard,
      collapse: false,
      id: "reports",
      label: "Reports",
      fullPath: getReportsPage(),
      icon: "assessments",
      actions: {},
      rbac: ["admin", "auditor", "limited_user", USERROLES.SUPER_ADMIN],
      settingsDescription: "View reports on Assessments, Plugins, Integrations and Playbook Reports.",
      settingsGroupId: 4,
      items: [
        {
          id: "reports",
          label: "Reports",
          path: "",
          description: "Detailed Report"
        },
        {
          id: "integration-reports",
          label: "Integration Reports",
          path: "/view-integration-report",
          description: "Detailed Report"
        }
      ]
    },
    ...configTableRoutes(),
    {
      name: "Reports",
      path: "/reports-view",
      layout: getBaseUrl(),
      rbac: ["admin", "auditor", USERROLES.SUPER_ADMIN],
      component: PluginResultContainer,
      //hide: options.reactAppDashboard,
      collapse: false
    },
    // {
    //   name: "Configurable Dashboard",
    //   path: "/configure-dashboard",
    //   layout: getBaseUrl(),
    //   component: ConfigurableDashboardContainer,
    //   rbac: USER_ADMIN_ROLES,
    //   collapse: false
    // },
    {
      name: "Integration Reports",
      path: "/reports/view-integration-report",
      layout: getBaseUrl(),
      rbac: ["admin", "auditor", USERROLES.SUPER_ADMIN],
      component: IntegrationReportDetails,
      collapse: false
    },

    {
      name: "SDLC FLOWS",
      path: "/sdlc-flows",
      layout: getBaseUrl(),
      rbac: USER_ADMIN_ROLES,
      component: SDLCFlows,
      hide: hideAppSDLCFlows,
      collapse: false
    },
    ...issuesRoutes(hideAppSTT),
    {
      path: "/view-reports",
      layout: getBaseUrl(),
      name: "Reports",
      mini: "Q",
      component: Reports,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: WORKSPACE_PATH,
      layout: getBaseUrl(),
      name: "Products",
      mini: "P",
      component: WorkspaceList,
      id: "products",
      label: WORKSPACES,
      fullPath: `${getBaseUrl()}/workspaces`,
      icon: "product",
      actions: {},
      rbac: USER_ADMIN_ROLES,
      items: [
        {
          id: "products",
          path: "",
          label: WORKSPACES,
          description: "Manage products that are used in this environment",
          hasAction: false,
          actionRoute: "add-product-page",
          actionLabel: "Add Product",
          dynamicHeader: true,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getBaseUrl()}${WORKSPACE_ADD_PATH}`));
          }
        }
      ]
    },
    {
      path: "/workflows/view-workflows",
      layout: getBaseUrl(),
      name: "List Tools",
      mini: "TT",
      component: WorkflowList,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/workflows",
      layout: getBaseUrl(),
      name: "List Tools",
      mini: "TT",
      component: WorkflowList,
      hide: hideAppWorkflows,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    ...propelRoutes(hideAppWorkflows),
    {
      path: "/reports/propel-report",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: PropelReportDetail,
      invisible: true,
      rbac: ["admin", "auditor", USERROLES.SUPER_ADMIN]
    },
    {
      path: "/configuration/integrations/add-integration-page",
      layout: getBaseUrl(),
      name: "Integrations",
      mini: "IN",
      component: IntegrationAdd,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/onboarding",
      layout: getBaseUrl(),
      name: "Onboarding",
      mini: "IN",
      component: ConnectTools,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/integrations/add-integration/jenkins",
      layout: getBaseUrl(),
      name: "Jenkins Integrations",
      mini: "IN",
      component: AddJenkinsIntegrationNodes,
      rbac: USER_ADMIN_ROLES,
      label: "Add Nodes",
      items: [
        {
          id: "jenkins",
          path: "",
          label: "Add Nodes",
          description: "",
          hasAction: false,
          actionRoute: "add-product-page",
          dynamicHeader: true
        }
      ]
    },
    {
      path: WORKSPACE_ADD_PATH,
      layout: getBaseUrl(),
      name: "Add Product",
      mini: "AP",
      component: ProductDetail,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: WORKSPACE_EDIT_PATH,
      layout: getBaseUrl(),
      name: "Edit Product",
      mini: "AP",
      component: ProductDetail,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/workflows/workflow-editor",
      layout: getBaseUrl(),
      name: "List Tools",
      mini: "TT",
      hide: hideAppWorkflows,
      component: WorkflowEditor,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/results",
      layout: getBaseUrl(),
      name: "List Results",
      mini: "TT",
      component: SignatureLogsList,
      hide: hideAppSignatures,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/login-page",
      layout: "/auth",
      name: "Login Page",
      mini: "LP",
      component: LoginPage,
      invisible: true
    },
    {
      path: "/not-found-page",
      layout: getBaseUrl(),
      name: "Page Not Found",
      mini: "404",
      component: ErrorNotFound,
      rbac: USER_ADMIN_ROLES,
      invisible: true
    },
    {
      path: "/reset-password",
      layout: "/auth",
      name: "Reset Password Page",
      mini: "RP",
      component: ResetPasswordPage,
      invisible: true
    },
    {
      collapse: true,
      path: "/pages",
      name: "Settings",
      state: "openPages",
      icon: "pe-7s-tools",
      invisible: true,
      views: [
        {
          path: "/configuration/user-page",
          layout: getBaseUrl(),
          name: "User Page",
          mini: "UP",
          component: ProfileEdit,
          invisible: true,
          rbac: USER_ADMIN_ROLES
        },
        {
          path: "/login-page",
          layout: "/auth",
          name: "Login Page",
          mini: "LP",
          component: LoginPage,
          invisible: true
        },
        {
          path: "/not-found-page",
          layout: getBaseUrl(),
          name: "Page Not Found",
          mini: "404",
          component: ErrorNotFound,
          rbac: USER_ADMIN_ROLES,
          invisible: true
        },
        {
          path: "/reset-password",
          layout: "/auth",
          name: "Reset Password Page",
          mini: "RP",
          component: ResetPasswordPage,
          invisible: true
        }
      ]
    },
    {
      name: "home",
      path: "/sei-home",
      layout: getBaseUrl(projectPathPropsDef),
      rbac: [...USER_ADMIN_ROLES, USERROLES.LIMITED_USER, USERROLES.PUBLIC_DASHBOARD, USERROLES.ORG_UNIT_ADMIN],
      mini: "home",
      component: LandingPageContainer,
      hide: hideAppDashboard,
      collapse: false,
      dynamicHeader: true,
      newHeader: LandingPageHeader,
      icon: "home",
      id: "home",
      label: "Home"
    },
    ...templateRoutes(hideAppSTT),
    ...triageRoutes(false),
    ...configurationRoutes({
      hideAppSignatures,
      hideAppViolation,
      hideAppTools
    }),
    {
      path: "/no-ou-dash",
      layout: getBaseUrl(),
      rbac: ["admin"],
      component: NoOUDefaultDashboard,
      hide: hideAppDashboard,
      newHeader: DashboardCustomOU,
      exact: false,
      dynamicHeader: true,
      collapse: false,
      showLabel: false,
      invisible: true
    }
  ];
}
export default routes;
