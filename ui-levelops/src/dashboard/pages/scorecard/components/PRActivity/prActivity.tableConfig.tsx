import React from "react";
import moment, { Moment } from "moment";
import { dots } from "shared-resources/charts/activity-table/dots";
import { ACTIVITY_COLORS, PR_STATUS, weekDays } from "shared-resources/charts/activity-table/helper";
import { transformKey } from "shared-resources/charts/helper";
import TooltipWithTruncatedTextComponent from "shared-resources/components/tooltip-with-truncated-text/TooltipWithTruncatedTextComponent";
import { baseColumnConfig } from "utils/base-table-config";
import { getDate } from "utils/dateUtils";
import { capitalizeWord, toTitleCase } from "utils/stringUtils";
import "./pr-activity.scss";

const defaultWeekColumns = (days: Array<Moment>, width?: string) =>
  days.map((day, index) => ({
    ...baseColumnConfig(day.format("ddd D MMM"), weekDays[index]),
    textAlign: "left",
    width: width || "120Px",
    render: (value: string, rowData: any) => dots(Number.parseInt(value), ACTIVITY_COLORS[rowData.type as PR_STATUS])
  }));

export const getPRActivityColumns = (
  selectedTimeRange: { $gt: string; $lt: string } | undefined,
  width?: string,
  leftEllipsis?: boolean
) => {
  const weekColumns: Array<Moment> = [];
  if (selectedTimeRange?.$gt) {
    for (let index = 0, date = getDate(selectedTimeRange.$gt); index < 7; index++) {
      weekColumns.push(moment(date));
      date.add(1, "day");
    }
  }
  return [
    {
      ...baseColumnConfig("name", "name"),
      width: "12%",
      textAlign: "left",
      render: (data: string, record: any, options?: any) => (
        <TooltipWithTruncatedTextComponent
          title={data}
          textClassName="pr-activity-name"
          allowedTextLength={25}
          leftEllipsis={leftEllipsis}
          hideTooltip={options?.hideData}
        />
      )
    },
    {
      ...baseColumnConfig("Totals", "type"),
      width: "150px",
      textAlign: "right",
      style: {
        color: "#2A67DD"
      },
      render: (data: string) => capitalizeWord(toTitleCase(transformKey(data)), "PR")
    },
    {
      ...baseColumnConfig("", ""),
      width: "20px",
      key: "color",
      style: {
        padding: "0 7px"
      },
      render: (data: any, rowData: any) => dots(1, ACTIVITY_COLORS[rowData.type as PR_STATUS])
    },
    {
      ...baseColumnConfig("", "total"),
      width: "40px",
      key: "total"
    },
    ...defaultWeekColumns(weekColumns, width)
  ];
};
