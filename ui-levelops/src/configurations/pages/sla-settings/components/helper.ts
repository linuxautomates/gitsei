export const typeKey = (application: string) => {
  switch (application) {
    case "jira":
      return "issue_type";
    case "azure_devops":
      return "workitem_type";
    default:
      return "issue_type";
  }
};

export const getPrioritiesUri = (application: string) => {
  switch (application) {
    case "jira":
      return "jira_priorities";
    case "azure_devops":
      return "workitem_priorities";
    default:
      return "jira_priorities";
  }
};
