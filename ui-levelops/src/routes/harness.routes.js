import SEIHomePage from "pages/home/SEIHomePage";
import { USER_ADMIN_ROLES } from "./helper/constants";
import { getBaseUrl } from 'constants/routePaths';
import SEITrialPage from "pages/home/SEITrialPage/SEITrialPage";
import GetStarted from 'pages/GetStarted/GetStarted';
import { projectPathPropsDef } from 'utils/routeUtils'

export const HarnessRoutes = () => [
  {
    path: "/home",
    layout: getBaseUrl(),
    name: "SEIHomePage",
    component: SEIHomePage,
    rbac: USER_ADMIN_ROLES,
    id: "seiHomePage",
    label: "SEI Home",
    fullPath: `${getBaseUrl()}/home`,
    actions: {},
  },
  {
    path: "/home/trial",
    layout: getBaseUrl(),
    name: "SEITrialPage",
    component: SEITrialPage,
    rbac: USER_ADMIN_ROLES,
    id: "seiTrailPage",
    label: "SEI Trail",
    fullPath: `${getBaseUrl()}/home/trial`,
    actions: {},
  }
];
