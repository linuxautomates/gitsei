import { baseColumnConfig } from "utils/base-table-config";
import { userColumn, statusColumn, priorityColumn, utcTimeColumn } from "./common-table-columns";
import { zendeskSalesForceHygieneTypes } from "./zendeskTableConfig";

export const SalesForceTableConfig = [
  baseColumnConfig("Account Name", "account_name"),
  userColumn("Contact", "contact"),
  userColumn("Creator", "creator"),
  baseColumnConfig("Origin", "origin"),
  statusColumn(),
  baseColumnConfig("Type", "type"),
  priorityColumn(),
  utcTimeColumn("Created At", "sf_created_at"),
  utcTimeColumn("Updated At", "sf_modified_at")
];

export const SalesForceTopCustomerTableConfig = [
  baseColumnConfig("Name", "key"),
  baseColumnConfig("Total Issues", "total_issues"),
  {
    ...baseColumnConfig("Status", "status", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Status",
    filterField: "statuses"
  },
  {
    ...baseColumnConfig("Priority", "priority", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Priority",
    filterField: "Priorities"
  },
  {
    ...baseColumnConfig("Type", "type", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Types",
    filterField: "types"
  },
  {
    ...baseColumnConfig("Contact", "contact", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Contacts",
    filterField: "contacts"
  },
  {
    ...baseColumnConfig("Account Name", "account", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Account Name",
    filterField: "accounts"
  },
  {
    ...baseColumnConfig("Hygiene", "id1", { hidden: true }),
    filterType: "multiSelect",
    filterField: "hygiene_types",
    options: zendeskSalesForceHygieneTypes,
    filterLabel: "Hygiene"
  }
];
