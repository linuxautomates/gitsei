import { baseColumnConfig } from "utils/base-table-config";
import { updatedAtColumn } from "utils/tableUtils";
import { dateRangeFilterColumn } from "./common-table-columns";
import { convertToDays } from "utils/timeUtils";

export const LevelOpsAssessmentTableConfig = [
  baseColumnConfig("Issue", "vanity_id"),
  baseColumnConfig("Assessment", "questionnaire_template_name", { width: "15%" }),
  { ...updatedAtColumn() }
];

export const LevelOpsAssessmentResponseTimeTableConfig = [
  baseColumnConfig("Key", "key"),
  {
    ...baseColumnConfig("Min", "min"),
    render: (item: any) => (item ? `${convertToDays(item)} days` : "")
  },
  {
    ...baseColumnConfig("Median", "median"),
    render: (item: any) => (item ? `${convertToDays(item)} days` : "")
  },
  {
    ...baseColumnConfig("Max", "max"),
    render: (item: any) => (item ? `${convertToDays(item)} days` : "")
  },
  {
    ...baseColumnConfig("Tags", "tags", { hidden: true }),
    filterTitle: "Tags",
    filterType: "apiMultiSelect",
    filterField: "tag",
    uri: "tags"
  },
  {
    ...baseColumnConfig("Assessments", "questionnaire_template_id", { hidden: true }),
    filterTitle: "Assessments",
    filterType: "apiMultiSelect",
    filterField: "questionnaire_template_id",
    uri: "questionnaires"
  },
  {
    ...baseColumnConfig("Progress", "completed", { hidden: true }),
    filterTitle: "Progress",
    filterType: "select",
    filterField: "completed",
    options: [
      { label: "Completed", value: true },
      { label: "Not Completed", value: false }
    ]
  },
  dateRangeFilterColumn("Update between", "updated_at"),
  dateRangeFilterColumn("Create between", "created_at")
];
