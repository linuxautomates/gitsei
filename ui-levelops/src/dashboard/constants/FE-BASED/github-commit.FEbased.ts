import moment from "moment";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { FEBasedFilterMap } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import { IssueVisualizationTypes, ScmCommitsMetricTypes } from "../typeConstants";

export enum LastFileUpdateTimestampValues {
  "30_DAYS" = "30_days",
  "2_MONTHS" = "2_months",
  "3_MONTHS" = "3_months",
  "6_MONTHS" = "6_months",
  "9_MONTHS" = "9_months",
  "12_MONTHS" = "12_months"
}

export const fileUpdateTimeStampOptions = [
  { label: "Older than 30 days ago", value: LastFileUpdateTimestampValues["30_DAYS"] },
  { label: "Older than 2 months", value: LastFileUpdateTimestampValues["2_MONTHS"] },
  { label: "Older than 3 months", value: LastFileUpdateTimestampValues["3_MONTHS"] },
  { label: "Older than 6 months", value: LastFileUpdateTimestampValues["6_MONTHS"] },
  { label: "Older than 9 months", value: LastFileUpdateTimestampValues["9_MONTHS"] },
  { label: "Older than 12 months", value: LastFileUpdateTimestampValues["12_MONTHS"] }
];

export const githubCommitsMetricOptions = [
  { label: "Number of Commits", value: ScmCommitsMetricTypes.NO_OF_COMMITS },
  { label: "Average Commit Size", value: ScmCommitsMetricTypes.AVERAGE_COMMIT_SIZE },
  { label: "Number of Lines Changed", value: ScmCommitsMetricTypes.NO_OF_LINES_CHANGED },
  { label: "Number of Lines Removed", value: ScmCommitsMetricTypes.NO_OF_LINES_REMOVED },
  { label: "Number of New Lines", value: ScmCommitsMetricTypes.NO_OF_NEW_LINES },
  { label: "Percentage of New Lines", value: ScmCommitsMetricTypes.PERECENTAGE_NEW_LINES },
  { label: "Percentage of Refactored Lines", value: ScmCommitsMetricTypes.PERCENTAGE_REFACTORED_LINES },
  {
    label: "Percentage of Legacy Refactored Lines",
    value: ScmCommitsMetricTypes.PERCENTAGE_LEGACY_REFACTORED_LINES
  }
];

export const LastFileUpdateTimestamp: { [x in LastFileUpdateTimestampValues]: string } = {
  [LastFileUpdateTimestampValues["30_DAYS"]]: moment().startOf("d").subtract(30, "d").unix().toString(),
  [LastFileUpdateTimestampValues["2_MONTHS"]]: moment().startOf("d").subtract(2, "M").unix().toString(),
  [LastFileUpdateTimestampValues["3_MONTHS"]]: moment().startOf("d").subtract(3, "M").unix().toString(),
  [LastFileUpdateTimestampValues["6_MONTHS"]]: moment().startOf("d").subtract(6, "M").unix().toString(),
  [LastFileUpdateTimestampValues["9_MONTHS"]]: moment().startOf("d").subtract(9, "M").unix().toString(),
  [LastFileUpdateTimestampValues["12_MONTHS"]]: moment().startOf("d").subtract(12, "M").unix().toString()
};

export const githubCommitsmetricsChartMapping: { [x in ScmCommitsMetricTypes]: string } = {
  [ScmCommitsMetricTypes.NO_OF_COMMITS]: "Commits",
  [ScmCommitsMetricTypes.AVERAGE_COMMIT_SIZE]: "Average Commit Size",
  [ScmCommitsMetricTypes.NO_OF_LINES_CHANGED]: "Lines Changed",
  [ScmCommitsMetricTypes.NO_OF_LINES_REMOVED]: "Lines Removed",
  [ScmCommitsMetricTypes.NO_OF_NEW_LINES]: "Lines added",
  [ScmCommitsMetricTypes.PERCENTAGE_REFACTORED_LINES]: "% Refactored Lines",
  [ScmCommitsMetricTypes.PERCENTAGE_LEGACY_REFACTORED_LINES]: "% Legacy Refactored Lines",
  [ScmCommitsMetricTypes.PERECENTAGE_NEW_LINES]: "% New Lines"
};

export const scmCommitsReportFEBased: FEBasedFilterMap = {
  visualization: {
    type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
    label: "Visualization",
    BE_key: "visualization",
    configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    options: [
      { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
      { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART },
      { label: "Pie Chart", value: IssueVisualizationTypes.PIE_CHART },
      { label: "Smooth Area Chart", value: IssueVisualizationTypes.AREA_CHART }
    ]
  }
};

export const scmCollabRadioBasedFilter: FEBasedFilterMap = {
  pr_filter: {
    type: WidgetFilterType.RADIO_BASED_FILTERS,
    label: "PR Filter",
    BE_key: "missing_fields",
    configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    options: [
      { label: "PR CLOSED", value: true },
      { label: "PR MERGED", value: false }
    ],
    getFilter: (value: boolean) => {
      return {
        pr_merged: value
      };
    },
    getValue: (filters: any, beKey: string) => {
      return filters[beKey]?.pr_merged;
    }
  }
};
