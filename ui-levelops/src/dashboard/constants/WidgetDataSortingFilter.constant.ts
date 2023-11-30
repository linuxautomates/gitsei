import { forEach } from "lodash";
import { optionType } from "dashboard/dashboard-types/common-types";

export enum widgetDataSortingOptionsNodeType {
  NON_TIME_BASED = "NON_SPRINT_TIME_BASED",
  TIME_BASED = "TIME_BASED",
  SPRINT_TIME_BASED = "SPRINT_TIME_BASED",
  AZURE_ITERATION_BASED = "AZURE_ITERATION_BASED",
  NON_TIME_METRIC_BASED = "NON_TIME_METRIC_BASED"
}

export enum widgetDataSortingOptionKeys {
  VALUE_LOW_HIGH = "value_low-high",
  VALUE_HIGH_LOW = "value_high-low",
  LABEL_LOW_HIGH = "label_low-high",
  LABEL_HIGH_LOW = "label_high-low",
  SPRINT_START_DATE_OLD_LATEST = "startDate_old-latest",
  SPRINT_START_DATE_LATEST_OLD = "startDate_latest-old",
  SPRINT_END_DATE_OLD_LATEST = "endDate_old-latest",
  SPRINT_END_DATE_LATEST_OLD = "endDate_latest-old",
  DEFAULT_OLD_LATEST = "default_old-latest",
  DEFAULT_LATEST_OLD = "default_latest-old"
}

export const widgetDataSortingOptions: { [x: string]: optionType[] } = {
  [widgetDataSortingOptionsNodeType.NON_TIME_BASED]: [
    { label: "By value, Low → High", value: widgetDataSortingOptionKeys.VALUE_LOW_HIGH },
    { label: "By value, High → Low", value: widgetDataSortingOptionKeys.VALUE_HIGH_LOW },
    { label: "By label, Low → High", value: widgetDataSortingOptionKeys.LABEL_LOW_HIGH },
    { label: "By label, High → Low", value: widgetDataSortingOptionKeys.LABEL_HIGH_LOW }
  ],
  [widgetDataSortingOptionsNodeType.TIME_BASED]: [
    { label: "Oldest → Latest", value: widgetDataSortingOptionKeys.DEFAULT_OLD_LATEST },
    { label: "Latest → Oldest", value: widgetDataSortingOptionKeys.DEFAULT_LATEST_OLD }
  ],
  [widgetDataSortingOptionsNodeType.SPRINT_TIME_BASED]: [
    { label: "By sprint start date, Oldest → Latest", value: widgetDataSortingOptionKeys.SPRINT_START_DATE_OLD_LATEST },
    { label: "By sprint start date, Latest → Oldest", value: widgetDataSortingOptionKeys.SPRINT_START_DATE_LATEST_OLD },
    { label: "By sprint end date, Oldest → Latest", value: widgetDataSortingOptionKeys.SPRINT_END_DATE_OLD_LATEST },
    { label: "By sprint end date, Latest → Oldest", value: widgetDataSortingOptionKeys.SPRINT_END_DATE_LATEST_OLD }
  ],
  [widgetDataSortingOptionsNodeType.AZURE_ITERATION_BASED]: [
    { label: "By sprint start date, Oldest → Latest", value: widgetDataSortingOptionKeys.SPRINT_START_DATE_OLD_LATEST },
    { label: "By sprint end date, Oldest → Latest", value: widgetDataSortingOptionKeys.SPRINT_END_DATE_OLD_LATEST }
  ],
  [widgetDataSortingOptionsNodeType.NON_TIME_METRIC_BASED]: [
    { label: "By label, Low → High", value: widgetDataSortingOptionKeys.LABEL_LOW_HIGH },
    { label: "By label, High → Low", value: widgetDataSortingOptionKeys.LABEL_HIGH_LOW }
  ]
};

export const widgetDataSortingOptionsDefaultValue = {
  [widgetDataSortingOptionsNodeType.NON_TIME_BASED]: widgetDataSortingOptionKeys.VALUE_HIGH_LOW,
  [widgetDataSortingOptionsNodeType.SPRINT_TIME_BASED]: widgetDataSortingOptionKeys.SPRINT_START_DATE_OLD_LATEST,
  [widgetDataSortingOptionsNodeType.TIME_BASED]: widgetDataSortingOptionKeys.DEFAULT_OLD_LATEST,
  [widgetDataSortingOptionsNodeType.NON_TIME_METRIC_BASED]: widgetDataSortingOptionKeys.LABEL_LOW_HIGH
};

export const allDataSortingOptions = () => {
  const allOptions: optionType[] = [];
  forEach(Object.keys(widgetDataSortingOptions), key => allOptions.push(...widgetDataSortingOptions[key]));
  return allOptions;
};
