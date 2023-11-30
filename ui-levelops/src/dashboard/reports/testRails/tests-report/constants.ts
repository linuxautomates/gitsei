import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const testRailsTestsReportChartProps = {
  barProps: [
    {
      name: "Total Tests",
      dataKey: "total_tests"
    }
  ],
  unit: "Tests",
  chartProps: chartProps,
  stacked: false
};

export const VISUALIZATION_OPTIONS = [
  { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
  { label: "Pie Chart", value: IssueVisualizationTypes.PIE_CHART },
];

export const REMOVE_COLUMN_KEY = ["status", "test_plan", "test_run", "defects"];
export const CUSTOM_CHECKBOX_FIELD_TYPE = "CHECKBOX";