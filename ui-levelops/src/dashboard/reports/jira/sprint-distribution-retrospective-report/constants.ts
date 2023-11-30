import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { toTitleCase } from "utils/stringUtils";

export const sprintDistributionRetrospectiveReportChartTypes = {
  useCustomToolTipHeader: (data: any[], key: any) => {
    const res = data.find(val => {
      return val.name === key;
    });
    return toTitleCase(res?.title) || "";
  },
  hideTotalInTooltip: true,
  stackOffset: "sign",
  unit: "Work",
  barProps: [
    {
      dataKey: "planned",
      fill: "#4197FF",
      name: "Planned",
      unit: "Work"
    },
    {
      dataKey: "unplanned",
      fill: "#FF4D4F",
      name: "Unplanned",
      unit: "Work"
    }
  ],
  stacked: true,
  chartProps: {
    ...chartProps
  }
};

export const sprintDistributionRetrospectiveReportInfo = {
  additional_key: "Sprint name",
  status: "Issue status at sprint close.",
  story_points: "Story points at sprint start and close."
};

export const sprint_end_date = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Sprint end date",
  BE_key: "completed_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  hideOnFilterValueKeys: ["state"]
};

export const sprintDefaultMeta = {
  [RANGE_FILTER_CHOICE]: {
    completed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "weeks"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const METRIC_OPTIONS = [
  { label: "Average of delivered Story Points", value: "story_points" },
  { label: "Average of delivered Ticket Count", value: "ticket_count" }
];
