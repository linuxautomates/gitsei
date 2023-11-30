import { SmartTickets } from "configurations/pages/smart-tickets";
import { BASE_UI_URL } from "helper/envPath.helper";
import { WorkItemsList } from "workitems/pages";
import { getWorkitemsPage, getWorkitemDetailPage, getBaseUrl } from "constants/routePaths";
import { USERROLES, ISSUE_ROUTES_RBAC } from "./helper/constants";

export const issuesRoutes = hideAppSTT => {
  return [
    {
      path: "/workitems",
      layout: getBaseUrl(),
      name: "Work Bench",
      mini: "W",
      component: WorkItemsList,
      rbac: ISSUE_ROUTES_RBAC,
      id: "workitems",
      label: "Issues",
      fullPath: getWorkitemsPage(),
      icon: "work",
      actions: {},
      items: [
        {
          id: "workitems",
          path: "",
          hasAction: true,
          label: "Issues",
          description: "View and edit work items",
          actionRoute: "",
          actionLabel: "New Issue",
          rbac: ["admin", "assigned_issues_user", USERROLES.SUPER_ADMIN],
          dynamicHeader: true,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getWorkitemDetailPage()}?new=1`));
          }
        },
        {
          hasAction: true,
          id: "details",
          rbac: ISSUE_ROUTES_RBAC,
          path: "/details",
          actionLabel: "New Issue",
          actionId: "add-issue",
          actionRoute: "add-issue",
          label: "Details",
          dynamicHeader: true,
          hide: hideAppSTT,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getWorkitemDetailPage()}?new=1`));
          }
        }
      ]
    },
    {
      name: "Work Items",
      path: "/workitems/details",
      layout: getBaseUrl(),
      rbac: ISSUE_ROUTES_RBAC,
      component: SmartTickets,
      hide: hideAppSTT,
      collapse: false,
      hasAction: true,
      actionLabel: "New Issue",
      actionId: "add-issue",
      actionRoute: "add-issue",
      buttonHandler: () => {
        window.location.replace(BASE_UI_URL.concat(`${getWorkitemDetailPage()}?new=1`));
      },
      items: [
        {
          hasAction: true,
          id: "details",
          rbac: ISSUE_ROUTES_RBAC,
          path: "",
          actionLabel: "New Issue",
          actionId: "add-issue",
          actionRoute: "add-issue",
          dynamicHeader: true,
          label: "Details",
          hide: hideAppSTT,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getWorkitemDetailPage()}?new=1`));
          }
        }
      ]
    }
  ];
};
