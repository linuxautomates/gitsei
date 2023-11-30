import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalIssueManagementSystemFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalIssueManagermentSystemfilter";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const supportManagementSystemFilterConfig: LevelOpsFilter = {
  id: "jira_zendesk_issue_management_system",
  renderComponent: UniversalIssueManagementSystemFilter,
  label: "Issue Management System",
  beKey: "support_system",
  labelCase: "title_case",
  disabled: (args: any) => {
    const { filters } = args || {};
    return !!filters?.["disable_support_system"];
  },
  defaultValue: "zendesk",
  getMappedValue: (args: any) => {
    const { allFilters } = args;
    return get(allFilters, ["support_system"], undefined) ?? get(allFilters, ["default_value"], undefined);
  },
  filterMetaData: {
    valueKey: "value",
    labelKey: "label",
    selectMode: "default",
    options: [
      { label: "Salesforce", value: "salesforce" },
      { label: "Zendesk", value: "zendesk" }
    ],
    sortOptions: true
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
