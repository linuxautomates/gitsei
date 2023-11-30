import { baseColumnConfig } from "utils/base-table-config";
import { timeColumn, statusColumn } from "./common-table-columns";

export const PagerdutyHotspotTableConfig = [
  baseColumnConfig("Pagerduty Name", "name"),
  baseColumnConfig("Type", "type")
];
