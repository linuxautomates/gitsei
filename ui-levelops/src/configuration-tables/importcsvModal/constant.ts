export enum TableColumnTypes {
  TEXT = "string",
  BOOLEAN = "boolean",
  DATE = "date",
  BASELINE = "base_line",
  PRESET = "single-select"
}

export const COLUMN_TYPE_OPTIONS = [
  {
    label: "Text (Default)",
    value: TableColumnTypes.TEXT
  },
  {
    label: "Boolean",
    value: TableColumnTypes.BOOLEAN
  },
  {
    label: "Date",
    value: TableColumnTypes.DATE
  },
  {
    label: "Baseline",
    value: TableColumnTypes.BASELINE
  },
  {
    label: "Preset",
    value: TableColumnTypes.PRESET
  }
];
