import { baseColumnConfig } from "../../../../utils/base-table-config";
import { coloredTagsColumn, timeColumn } from "./common-table-columns";

export const CoverityDefectTableConfig = [
  { ...baseColumnConfig("Component", "component_name"), filterField: "cov_defect_component_names" },
  { ...coloredTagsColumn("Impact", "impact"), filterField: "cov_defect_impacts" },
  { ...baseColumnConfig("Category", "category"), filterField: "cov_defect_categories" },
  { ...baseColumnConfig("Kind", "kind"), filterField: "cov_defect_kinds" },
  { ...baseColumnConfig("Checker Name", "checker_name"), filterField: "cov_defect_checker_names" },
  { ...baseColumnConfig("Domain", "domain"), filterField: "cov_defect_domains" },
  baseColumnConfig("Occurrence Count", "occurrence_count"),
  { ...baseColumnConfig("Type", "type"), filterField: "cov_defect_types" },
  baseColumnConfig("Function Name", "function_name"),
  {
    ...timeColumn("First Detected At", "first_detected_at"),
    align: "left"
  },
  {
    ...timeColumn("Last Detected At", "last_detected_at"),
    align: "left"
  }
];
