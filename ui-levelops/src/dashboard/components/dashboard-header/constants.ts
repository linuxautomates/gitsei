import { DashboardType } from "configurations/configuration-types/OUTypes";
import { DynamicGraphFilter } from "../../constants/applications/levelops.application";

export const workItemSupportedFilters: Array<DynamicGraphFilter> = [
  {
    label: "Project",
    filterType: "apiMultiSelect",
    filterField: "product_ids",
    uri: "products",
    position: "left"
  },
  {
    label: "Reporter",
    filterType: "apiMultiSelect",
    filterField: "reporter",
    uri: "users",
    searchField: "email",
    position: "left"
  },
  {
    label: "Assignee",
    filterType: "apiMultiSelect",
    filterField: "assignee_user_ids",
    uri: "users",
    searchField: "email",
    position: "left",
    labelKey: "email",
    valueKey: "id"
  },
  {
    label: "Status",
    filterType: "apiMultiSelect",
    filterField: "status",
    searchField: "name",
    uri: "states",
    position: "left"
  },
  { label: "Tags", filterType: "apiMultiSelect", position: "left", filterField: "tag_ids", uri: "tags" },
  { label: "Updated Between", filterType: "dateRange", position: "right", filterField: "updated_at" }
];

export const iconMapping: DashboardType = {
  alignment: "target",
  planning: "calendar",
  execution: "zap"
};

export const NO_CHILD_OU_MSG = "This Collection does not have any child Collections.";

// hacky temp fix for rockwell
export const TENANTS_USING_PRE_CALC = ["rockwellautomation"];
