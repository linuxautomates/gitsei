export const OUFiltersMapping = {
  jira: [
    {
      label: "Reporter",
      value: "reporter"
    },
    {
      label: "Assignee",
      value: "assignee"
    }
  ],
  azure_devops: [
    { label: "Workitem Project", value: "workitem_project" },
    { label: "Workitem Status", value: "workitem_status" },
    { label: "Workitem Priority", value: "workitem_priority" },
    { label: "Workitem Type", value: "workitem_type" },
    { label: "Workitem Status Category", value: "workitem_status_category" },
    { label: "Workitem Parent Workitem Id", value: "workitem_parent_workitem_id" },
    { label: "Workitem Epic", value: "workitem_epic" },
    { label: "Workitem Assignee", value: "workitem_assignee" },
    { label: "Workitem Version", value: "workitem_version" },
    { label: "Workitem Fix Version", value: "workitem_fix_version" },
    { label: "Workitem Reporter", value: "workitem_reporter" },
    { label: "Workitem Label", value: "workitem_label" }
  ],
  github: [
    { label: "Repo", value: "repo_id" },
    {
      label: "Assignee",
      value: "assignee"
    },
    {
      label: "Approver",
      value: "approver"
    },
    {
      label: "Author",
      value: "author"
    },
    {
      label: "Reviewer",
      value: "reviewer"
    },
    {
      label: "Committer",
      value: "committer",
      defaultValue: ["committer"], // setting default value
      disabled: false
    }
  ],
  jenkins: [
    {
      label: "User Name",
      value: "cicd_user_id"
    }
  ]
} as Record<string, any>;
