import { LogAnnotate } from "../failure-analysis/pages";
import { USER_ADMIN_ROLES } from "./helper/constants";
import { getBaseUrl } from 'constants/routePaths';

export const FailureAnalysisRoutes = () => [
  {
    path: "/failure-logs",
    layout: getBaseUrl(),
    name: "Failure Logs",
    mini: "D",
    component: LogAnnotate,
    rbac: USER_ADMIN_ROLES,
    id: "failurelogs",
    label: "Failure Log",
    fullPath: `${getBaseUrl()}/failure-logs`,
    actions: {},
    items: [
      {
        path: "",
        name: "Failure Logs",
        rbac: USER_ADMIN_ROLES,
        dynamicHeader: true,
        hasAction: false
      }
    ]
  }
];
