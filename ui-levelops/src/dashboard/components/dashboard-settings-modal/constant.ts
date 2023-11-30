export const EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID = "default_config";
export const INTEGRATION_MONITORING_KEY = "integration_monitoring";

export const PERMISSION_DESC = {
  admin_only: {
    header: "Grant Admin-only access",
    subHeader: "Allow all admins to view and edit this insight."
  },
  public_view: {
    header: "Grant public view access, and edit access for Admins",
    subHeader: "Allow everyone in my collection to view, and allow all admins to edit this insight."
  },
  restricted: {
    header: "Restrict access to specific users, and optionally grant public view access",
    subHeader:
      "Add specific users as viewers or editors or owners to this insight, and optionally allow everyone in my collection to view this dasbboard.",
    note: "Note: Only insight creators or users with owner role access will be able to change permissions for the insight."
  }
};
