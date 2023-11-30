import { BULLSEYE_REPORTS } from "dashboard/constants/applications/names";
import { toTitleCase } from "utils/stringUtils";

export const buildMetricOptions = (key: "functions" | "branches" | "decisions") => {
  const valueMapping = {
    functions: "function",
    branches: "condition",
    decisions: "decision"
  };
  return [
    { label: `Percentage of Covered ${toTitleCase(key)}`, value: `${valueMapping[key]}_percentage_coverage` },
    { label: `Percentage of Uncovered ${toTitleCase(key)}`, value: `${valueMapping[key]}_percentage_uncovered` },
    { label: `Number of Covered ${toTitleCase(key)}`, value: `${key === "branches" ? "conditions" : key}_covered` },
    { label: `Number of Uncovered ${toTitleCase(key)}`, value: `${key === "branches" ? "conditions" : key}_uncovered` }
  ];
};

export const getOptionKey = (reportType: string) => {
  if (
    [BULLSEYE_REPORTS.BULLSEYE_BRANCH_COVERAGE_REPORT, BULLSEYE_REPORTS.BULLSEYE_BRANCH_COVERAGE_TREND_REPORT].includes(
      reportType as any
    )
  ) {
    return "branches";
  }
  if (
    [
      BULLSEYE_REPORTS.BULLSEYE_DECISION_COVERAGE_REPORT,
      BULLSEYE_REPORTS.BULLSEYE_DECISION_COVERAGE_TREND_REPORT
    ].includes(reportType as any)
  ) {
    return "decisions";
  }
  if (
    [
      BULLSEYE_REPORTS.BULLSEYE_FUNCTION_COVERAGE_REPORT,
      BULLSEYE_REPORTS.BULLSEYE_FUNCTION_COVERAGE_TREND_REPORT
    ].includes(reportType as any)
  ) {
    return "functions";
  }
};
