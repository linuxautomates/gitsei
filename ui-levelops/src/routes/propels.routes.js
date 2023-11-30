import { BASE_UI_URL } from "helper/envPath.helper";
import {
  PropelLandingPage,
  WorkflowEditor,
  RunsLogsList,
  AutomationRulesPage,
  AutomationRulesEditPage
} from "workflow";
import StringsEn from "../locales/StringsEn";
import { USER_ADMIN_ROLES } from "./helper/constants";
import { getBaseUrl } from 'constants/routePaths';
import withHarnessPermission from "hoc/withHarnessPermission";

export const propelRoutes = hideAppWorkflows => {
  return [
    {
      path: "/propels",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: withHarnessPermission(PropelLandingPage),
      invisible: true,
      rbac: USER_ADMIN_ROLES,
      id: "propels",
      fullPath: `${getBaseUrl()}/propels`,
      label: StringsEn.propels,
      icon: "workflows",
      items: [
        {
          hasAction: false,
          id: "propels-editor",
          rbac: USER_ADMIN_ROLES,
          path: "/propels-editor",
          label: StringsEn.propelsEditor,
          description: "Lorem Ipsum",
          showBreadcrumb: true,
          dynamicHeader: true
        },
        {
          id: "propels",
          path: "",
          rbac: USER_ADMIN_ROLES,
          label: StringsEn.propels,
          description: "Lorem Ipsum",
          hasAction: false,
          actionId: "add-workflow",
          actionRoute: "workflow-editor",
          actionLabel: StringsEn.propelAdd,
          dynamicHeader: true,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getBaseUrl()}/workflows/workflow-editor`));
          }
        },
        {
          id: "propels-runs-logs",
          path: "/runs-logs",
          rbac: USER_ADMIN_ROLES,
          label: StringsEn.PropelsRunsLogs,
          dynamicHeader: true
        },
        {
          path: "/automation-rules",
          name: "Automation Rules",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true,
          hasAction: true
        },
        {
          path: "/automation-rules/create",
          name: "Manage Automation Rule",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true,
          hasAction: true
        },
        {
          path: "/automation-rules/edit",
          name: "Manage Automation Rule",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true,
          hasAction: true
        }
      ]
    },
    {
      path: "/propels/propels-editor",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: withHarnessPermission(WorkflowEditor),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/propels/runs-logs",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: withHarnessPermission(RunsLogsList),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/propels/automation-rules",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: withHarnessPermission(AutomationRulesPage),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/propels/automation-rules/create",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: withHarnessPermission(AutomationRulesEditPage),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/propels/automation-rules/edit",
      layout: getBaseUrl(),
      name: "Runbooks",
      mini: "TT",
      hide: hideAppWorkflows,
      component: withHarnessPermission(AutomationRulesEditPage),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    }
  ];
};
