export const INTEGRATION_FIELD_NUMERIC_TYPES = ["number", "double"];

export const AZURE_TOGGLE_HEADING = "Please select one/more Azure devOps services from the below list.";

export const workItemFieldForAzureMapping: { [name: string]: string } = {
  workitem_project: "project",
  workitem_status: "status",
  workitem_priority: "priority",
  workitem_type: "workitem_type",
  workitem_status_category: "status_category",
  workitem_parent_workitem_id: "parent_workitem_id",
  workitem_epic: "epic",
  workitem_assignee: "assignee",
  workitem_version: "version",
  workitem_fix_version: "fix_version",
  workitem_reporter: "reporter",
  workitem_label: "label"
};
