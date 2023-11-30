import { USERROLES } from "routes/helper/constants";
import LocalStoreService from "services/localStoreService";
const ls = new LocalStoreService();
const lastWeek = Math.ceil(new Date().getTime() / 1000 - 604800);
const endDateTimeStamp = Math.floor(Date.now() / 1000);

export const ISSUES_RBAC_FULL_ACCESS = [
  USERROLES.ADMIN,
  USERROLES.ASSIGNED_ISSUES_USER,
  USERROLES.LIMITED_USER,
  USERROLES.SUPER_ADMIN
];

export const tabs = [
  {
    id: "all",
    label: "All",
    filters: {},
    rbac: ISSUES_RBAC_FULL_ACCESS
  },
  {
    id: "assigned-to-me",
    label: "Assigned To Me",
    filters: {
      assignee_user_ids: [ls.getUserId()]
    },
    rbac: ISSUES_RBAC_FULL_ACCESS
  },
  {
    id: "reported-by-me",
    label: "Reported By Me",
    filters: {
      reporter: ls.getUserEmail()
    },
    rbac: ISSUES_RBAC_FULL_ACCESS
  },
  {
    id: "new",
    label: "Since Last Week",
    filters: {
      created_after: lastWeek,
      updated_at: { $gt: endDateTimeStamp - 604800, $lt: endDateTimeStamp }
    },
    rbac: [USERROLES.ADMIN, USERROLES.LIMITED_USER, USERROLES.SUPER_ADMIN]
  },
  {
    id: "open",
    label: "Open",
    filters: {
      status: "OPEN"
    },
    rbac: [USERROLES.ADMIN, USERROLES.LIMITED_USER, USERROLES.SUPER_ADMIN]
  },
  {
    id: "unassigned",
    label: "Unassigned",
    filters: {
      unassigned: true
    },
    rbac: [USERROLES.ADMIN, USERROLES.LIMITED_USER, USERROLES.SUPER_ADMIN]
  }
];
