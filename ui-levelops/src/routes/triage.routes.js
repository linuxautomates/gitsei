import { TRIAGE_ROUTES, getBaseUrl } from "../constants/routePaths";
import { TriageDetail, TriageLandingPage, TriageRulesEditPage } from "../triage/pages";
import { navigateToRoute } from "../utils/routeUtils";
import { JobStagesPage } from "../triage/pages/grid-view/JobStagesPage";
import TriageResultsListPage from "../triage/pages/triageResultsList.page";
import { USER_ADMIN_ROLES } from "./helper/constants";

export function triageRoutes(hideTriage) {
  return [
    {
      name: "Triage",
      path: TRIAGE_ROUTES._ROOT,
      layout: getBaseUrl(),
      rbac: USER_ADMIN_ROLES,
      component: TriageLandingPage,
      collapse: false,
      id: "triage",
      label: "Triage",
      icon: "violationLogs",
      actions: {},
      items: [
        {
          label: "Triage",
          path: "",
          icon: "",
          description: "Pre-deployment Triage",
          actionId: "add-triage-rule",
          actionRoute: "triage/rules/create"
        },
        {
          // List
          //path: TRIAGE_ROUTES.LIST,
          path: "/rules",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage Triage Rules",
          label: "Triage Rules",
          hasAction: true,
          actionId: "add-triage-rule",
          actionRoute: "triage/rules/create",
          actionLabel: "Add Triage Rule",
          buttonHandler: () => navigateToRoute(getBaseUrl(), TRIAGE_ROUTES.CREATE)
        },
        {
          // Edit
          //path: TRIAGE_ROUTES.EDIT,
          path: "/rules/edit",
          name: "Edit Triage Rule",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true,
          hasAction: true
        },
        {
          // Create
          //path: TRIAGE_ROUTES.CREATE,
          path: "/rules/create",
          name: "Add Triage Rule",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true,
          hasAction: true
        },
        {
          // View
          //path: TRIAGE_ROUTES.DETAIL,
          path: "/rules/view",
          name: "View Triage Log",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // View
          //path: TRIAGE_ROUTES.DETAIL,
          path: "/view",
          name: "Triage Detail View",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // View
          // path: TRIAGE_ROUTES.RESULTS
          path: "/results",
          name: "Triage Results",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: false,
          hasAction: false,
          label: "Triage Results",
          withBackButton: true,
          showBottomSeparator: true
        }
      ]
    },
    {
      path: TRIAGE_ROUTES.RESULTS,
      layout: getBaseUrl(),
      name: "Triage Results",
      mini: "TR",
      component: TriageResultsListPage,
      rbac: USER_ADMIN_ROLES,
      id: "triage-results-list",
      description: "",
      label: "Triage Results"
    },
    {
      // Assessment Templates List
      path: TRIAGE_ROUTES.LIST,
      layout: getBaseUrl(),
      name: "Triage Rules",
      mini: "TR",
      component: TriageLandingPage,
      rbac: USER_ADMIN_ROLES,
      id: "view-triage-rules",
      description: "",
      label: "Triage Rules",
      icon: "signatures"
    },
    {
      // Assessment Templates List
      path: TRIAGE_ROUTES.EDIT,
      layout: getBaseUrl(),
      name: "Triage Rules Edit",
      mini: "TR",
      component: TriageRulesEditPage,
      rbac: USER_ADMIN_ROLES,
      id: "edit-triage-rules",
      description: "",
      label: "Edit Triage Rule",
      icon: "signatures",
      dynamicHeader: true,
      hasAction: true
    },
    {
      // Assessment Templates List
      path: TRIAGE_ROUTES.CREATE,
      layout: getBaseUrl(),
      name: "Triage Rules Create",
      mini: "TR",
      component: TriageRulesEditPage,
      rbac: USER_ADMIN_ROLES,
      id: "create-triage-rules",
      description: "",
      label: "Create Triage Rule",
      icon: "signatures",
      dynamicHeader: true,
      hasAction: true
    },
    {
      // Assessment Templates List
      path: TRIAGE_ROUTES.DETAIL,
      layout: getBaseUrl(),
      name: "Triage Rules Create",
      mini: "TR",
      component: TriageDetail,
      rbac: USER_ADMIN_ROLES,
      id: "create-triage-rules",
      description: "",
      label: "Create Triage Rule",
      icon: "signatures",
      dynamicHeader: true,
      hasAction: false
    },
    {
      // Assessment Templates List
      path: TRIAGE_ROUTES.JOB_STAGES,
      layout: getBaseUrl(),
      name: "Job Stages",
      mini: "TR",
      component: JobStagesPage,
      rbac: USER_ADMIN_ROLES,
      id: "view-job-stages",
      description: "",
      label: "Job Stages",
      icon: "signatures"
    }
  ];
}
