import { baseColumnConfig } from "utils/base-table-config";
import { coloredTagsColumn, statusColumn, timeColumn } from "./common-table-columns";

export const NccGroupTableConfig = [
  baseColumnConfig("Name", "name"),
  statusColumn("Status", "status", { width: "7%" }),
  baseColumnConfig("Risk", "risk", { width: "7%" }),
  baseColumnConfig("Category", "category"),
  baseColumnConfig("Impact", "impact"),
  {
    ...baseColumnConfig("Component", "component"),
    filterTitle: "Components",
    filterType: "apiMultiSelect",
    filterField: "component"
  },
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
  },
  timeColumn("Ingested At", "ingested_at")
];
