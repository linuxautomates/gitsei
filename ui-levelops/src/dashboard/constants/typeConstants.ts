export enum IssueVisualizationTypes {
  BAR_CHART = "bar_chart",
  STACKED_BAR_CHART = "stacked_bar_chart",
  DONUT_CHART = "donut_chart",
  LINE_CHART = "line_chart",
  PIE_CHART = "pie_chart",
  AREA_CHART = "area_chart",
  STACKED_AREA_CHART = "stacked_area_chart",
  PERCENTAGE_STACKED_BAR_CHART = "percentage_stacked_bar_chart"
}
export enum ScmCodeCategoryTypes {
  NEW_CODE = "new_code",
  REFACTORED_CODE = "refactored_code",
  LEGACY_REFACTORED_CODE = "legacy_refactored_code"
}

export enum ScmCommitsMetricTypes {
  NO_OF_COMMITS = "count",
  AVERAGE_COMMIT_SIZE = "avg_commit_size",
  NO_OF_LINES_CHANGED = "tot_lines_changed",
  NO_OF_LINES_REMOVED = "tot_lines_removed",
  NO_OF_NEW_LINES = "tot_lines_added",
  PERECENTAGE_NEW_LINES = "pct_new_lines",
  PERCENTAGE_REFACTORED_LINES = "pct_refactored_lines",
  PERCENTAGE_LEGACY_REFACTORED_LINES = "pct_legacy_refactored_lines"
}

export enum SCMVisualizationTypes {
  BAR_CHART = "bar_chart",
  CIRCLE_CHART = "circle_chart",
  LINE_CHART = "line_chart",
  AREA_CHART = "area_chart",
  STACKED_AREA_CHART = "stacked_area_chart"
}

export enum SCMReworkVisualizationTypes {
  STACKED_BAR_CHART = "stacked_bar_chart",
  PERCENTAGE_STACKED_BAR_CHART = "percentage_stacked_bar_chart"
}
