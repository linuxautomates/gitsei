import envConfig from "env-config";
import { RestDashboard } from "classes/RestDashboards";
import LocalStoreService from "services/localStoreService";

export const ALLOWED_LIMIT = 100;

export const ALL_ACCESS_USERS = (envConfig.get("ALL_ACCESS_USERS") || "").split(",");

export enum onOkTextType {
  UPDATE = "Update",
  CREATE = "Create"
}

export enum DashboardSettingsModalTitleType {
  DASHBOARD_SETTINGS = "Insight Settings",
  CREATE_DASHBOARD = "Create Insight",
  NAME = "Name",
  DEFAULT_DASHBOARD = "Default Insight",
  PERMISSIONS = "Permissions",
  PARAMETERS = "Parameters"
}

export enum DASHBOARD_SETTINGS_PERMISSIONS {
  ADMIN = "admin",
  PUBLIC = "public",
  LIMITED = "limited"
}
export enum DASHBOARD_SETTINGS_USER_PERMISSIONS {
  VIEW = "view",
  EDIT = "edit",
  OWNER = "owner"
}
export enum DashboardSettingsModalRadioHoverText {
  ADMIN = "Choose this option if you want to allow all users having Admin role access to view and edit this insight.",
  PUBLIC = "Choose this option if you want to allow all users in your collection to view this insight. In addition, users with Admin role access can edit the insight.",
  PRIVATE = "Choose this option if you want to allow specific users having Admin role access to view or edit this insight, or add them as owners to this insight so that they can manage permissions as well for this insight.",
  OWNER = "Admin users added as owners will be able to modify access permissions for this insight.",
  ALL_USER_CHECKOBOX = "Choose this checkbox if you want to provide view-only access to all users. You can still add specific users as editors or owners for this insight."
}
export const dashboardAccess = (dashboard: RestDashboard) => {
  const ls = new LocalStoreService();
  const access = {
    view: true,
    edit: true,
    owner: true
  };
  if (dashboard) {
    const userEmail: any = ls.getUserEmail();

    // Checking for emails/users that always have full access
    if (ALL_ACCESS_USERS.includes(userEmail) || userEmail === dashboard.metadata?.rbac?.owner) {
      return { view: true, edit: true, owner: true };
    }
    if (
      dashboard.metadata?.rbac?.isPublic === DASHBOARD_SETTINGS_PERMISSIONS.LIMITED ||
      dashboard.metadata?.rbac?.dashboardPermission === DASHBOARD_SETTINGS_PERMISSIONS.LIMITED
    ) {
      if (
        userEmail !== dashboard.metadata?.rbac?.owner &&
        !dashboard.metadata?.rbac?.users?.[userEmail] &&
        (dashboard.metadata?.rbac?.allUsers === false || dashboard.metadata?.rbac?.allUsers === undefined)
      ) {
        return { ...access, view: false, edit: false, owner: false };
      }
      if (dashboard.metadata?.rbac?.users?.[userEmail]) {
        const permission = dashboard.metadata?.rbac?.users?.[userEmail]?.permission;
        switch (permission !== null) {
          case permission === DASHBOARD_SETTINGS_USER_PERMISSIONS.OWNER: {
            return { ...access, view: true, edit: true, owner: true };
          }
          case permission === DASHBOARD_SETTINGS_USER_PERMISSIONS.EDIT: {
            return { ...access, view: true, edit: true, owner: false };
          }
          case permission === DASHBOARD_SETTINGS_USER_PERMISSIONS.VIEW: {
            return { ...access, view: true, edit: false, owner: false };
          }
        }
      } else if (dashboard.metadata?.rbac?.allUsers) {
        return { ...access, view: true, edit: false, owner: false };
      }
    }
  }
  return access;
};
