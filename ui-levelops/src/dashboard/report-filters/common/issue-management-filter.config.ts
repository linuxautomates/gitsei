import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalIssueManagementSystemFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalIssueManagermentSystemfilter";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const IssueManagementSystemFilterConfig: LevelOpsFilter = {
  id: "issue_management_system",
  renderComponent: UniversalIssueManagementSystemFilter,
  label: "Issue Management System",
  beKey: "issue_management_system",
  labelCase: "title_case",
  disabled: (args: any) => {
    const { filters } = args || {};
    return !!filters?.["disable_issue_management_system"];
  },
  getMappedValue: (args: any) => {
    const { allFilters } = args;
    const application = get(allFilters, ["reportApplication"]);
    if (application) {
      return application.includes("azure_devops") ? "azure_devops" : "jira";
    }
  },
  defaultValue: "jira",
  filterMetaData: {
    selectMode: "default",
    options: [
      { label: "Azure", value: "azure_devops" },
      { label: "Jira", value: "jira" }
    ],
    sortOptions: true
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
