import React from "react";
import { baseColumnConfig } from "utils/base-table-config";
import { priorityColumn, statusColumn, coloredTagsColumn } from "./common-table-columns";

export const PraetorianTableConfig = [
  baseColumnConfig("Name", "name"),
  statusColumn("Status", "status", { width: "5%" }),
  priorityColumn("Priority", "priority", { width: "5%" }),
  baseColumnConfig("Category", "category", { width: "7%" }),
  baseColumnConfig("Report Grade", "report_grade", { width: "7%" }),
  baseColumnConfig("Report Security", "report_security"),
  baseColumnConfig("Service", "service"),

  {
    ...coloredTagsColumn("Tags", "tags"),
    filterTitle: "Tags",
    filterType: "apiMultiSelect",
    filterField: "tag",
    uri: "tags"
  },
  {
    ...coloredTagsColumn("Projects", "projects"),
    filterTitle: "Projects",
    filterType: "apiMultiSelect",
    filterField: "project",
    uri: "products"
  }
];
