import { HIDE_REPORT } from "./filter-key.mapping";

export const NotesWidget = {
  dashboard_notes: {
    name: "Documentation Header",
    application: "dashboard_notes",
    supported_filters: [],
    [HIDE_REPORT]: true
  }
};

// for storing dashboard selected OU
export const DASHBOARD_OU_FIELD = "ou_ids";
